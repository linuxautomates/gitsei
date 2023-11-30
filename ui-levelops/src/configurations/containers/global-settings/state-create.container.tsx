import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";
import { Form } from "antd";
import { RestState } from "classes/RestState";
import { stateCreateState, stateGetState, stateUpdateState } from "reduxConfigs/selectors/statesSelector";
import { formClear, formInitialize, formUpdateField, formUpdateObj } from "reduxConfigs/actions/formActions";
import { statesCreate, statesGet, statesUpdate } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { getStateForm } from "reduxConfigs/selectors/formSelector";
import { restAPILoadingState } from "utils/stateUtil";
import { AntForm, AntFormItem, AntInput, AntModal } from "shared-resources/components";

interface StateCreateContainerProps {
  onOk: (settings: any) => void;
  onCancel: any;
  form?: any;
  stateId?: string;
}

export const StateCreateContainer: React.FC<StateCreateContainerProps> = (props: StateCreateContainerProps) => {
  const STATE_FORM_NAME = "state_form";

  let stateId = props.stateId;
  let isEditMode = !!stateId;

  const [creating, setCreating] = useState(false);
  const [loading, setLoading] = useState(isEditMode);

  const dispatch = useDispatch();
  const stateCreate = useSelector(stateCreateState);
  const stateUpdate = useSelector(stateUpdateState);
  const stateGet = useSelector(stateGetState);

  const stateForm = useSelector(getStateForm);

  useEffect(() => {
    dispatch(formInitialize(STATE_FORM_NAME, {}));

    if (isEditMode) {
      dispatch(statesGet(stateId));
    }
    return () => {
      dispatch(formClear(STATE_FORM_NAME));
      dispatch(restapiClear("states", "get", -1));
    };
  }, [statesGet]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loading && stateId !== undefined) {
      const { loading, error } = restAPILoadingState(stateGet, stateId);
      if (!loading && !error) {
        const stage = new RestState(get(stateGet, [stateId, "data"], {}));
        dispatch(formUpdateField(STATE_FORM_NAME, "name", stage.name));
        setLoading(false);
      }
    }
  }, [stateGet]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (creating) {
      let isLoading = true;
      let states = undefined;
      const method = !isEditMode ? "create" : "update";
      const { loading, error } = restAPILoadingState(!isEditMode ? stateCreate : stateUpdate, stateId);
      if (!loading) {
        isLoading = false;
        if (!error) {
          states = get(isEditMode ? stateUpdate : stateCreate, [stateId || "0", "data"], {});
        }
        setCreating(isLoading);
        dispatch(restapiClear("states", method, -1));
        props.onOk(states);
      }
    }
  }, [stateCreate, stateUpdate]); // eslint-disable-line react-hooks/exhaustive-deps

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      const globalSetting = Object.assign(Object.create(Object.getPrototypeOf(stateForm)), stateForm);
      globalSetting[field] = e.target ? e.target.value : e;
      dispatch(formUpdateObj(STATE_FORM_NAME, globalSetting));
    };
  };

  const onCancel = () => {
    dispatch(formClear(STATE_FORM_NAME));
    props.form.resetFields();
    props.onCancel();
  };

  const onOK = () => {
    let stateObj = new RestState(stateForm);
    setCreating(true);

    if (isEditMode) {
      dispatch(statesUpdate(stateId, stateObj));
    } else {
      dispatch(statesCreate(stateObj));
    }
    dispatch(formClear(STATE_FORM_NAME));
    props.form.resetFields();
  };
  const { getFieldDecorator, getFieldError, isFieldTouched } = props.form;
  return (
    <AntModal
      title={isEditMode ? "Update State" : "Create State"}
      visible
      onOk={onOK}
      okText="Save"
      onCancel={onCancel}
      closable={false}
      okButtonProps={{
        disabled: !stateForm.name?.length
      }}>
      <AntForm layout="vertical">
        <AntFormItem label={"State"} validateStatus={isFieldTouched("name") && getFieldError("name") ? "error" : ""}>
          {getFieldDecorator("name", {
            initialValue: stateForm.name || "",
            validateTrigger: "onBlur",
            rules: [
              {
                required: true,
                message: "This field cannot be empty"
              }
            ]
          })(<AntInput data-testid="state-name" onChange={onFieldChangeHandler("name")} />)}
        </AntFormItem>
      </AntForm>
    </AntModal>
  );
};

const StateCreateForm = Form.create({ name: "templates_add" })(StateCreateContainer);

export default StateCreateForm;
