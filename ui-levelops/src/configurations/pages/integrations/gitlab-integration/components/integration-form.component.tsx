import * as React from "react";
import { useEffect, useMemo, useState } from "react";
import { IntegrationAuthTypes } from "../helpers";
import { useCallback } from "react";
import { get, debounce } from "lodash";
import { EMPTY_FIELD_WARNING, ERROR, SUCCESS, VALIDATING } from "../../../../../constants/formWarnings";
import { Form, Input } from "antd";
import { SelectRestapi } from "../../../../../shared-resources/helpers";
import { tagsList, integrationsList } from "reduxConfigs/actions/restapi";
import { IntegrationMetaFields } from "../../../../containers/integration-steps/integrations-details-new/integration-meta-fields.component";
// @ts-ignore
import uuidv1 from "uuid/v1";
import { useDispatch, useSelector } from "react-redux";
import { checkTemplateNameExists } from "configurations/helpers/checkTemplateNameExits";
import { applicationType } from "../../integration-table-config";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import { BITBUCKET_REPO_DESC } from "constants/integration.description";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IntegrationFormProps {
  type: string;
  sub_type: IntegrationAuthTypes;
  integration_form: any;
  updateField: any;
  onValidation: any;
  width?: string;
  fieldInfo: any;
  setFieldInfo: any;
  fieldTouched: any;
  setFieldTouched: any;
}

export const IntegrationDetailsNewForm: React.FC<IntegrationFormProps> = ({
  type,
  sub_type,
  integration_form,
  updateField,
  onValidation,
  width = "40vw",
  fieldInfo,
  setFieldInfo,
  fieldTouched,
  setFieldTouched,
  ...props
}) => {
  const dispatch = useDispatch();

  const integrationListState = useSelector(state => get(state, ["restapiReducer", "integrations", "list"], {}));

  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [nameChecking, setNameChecking] = useState(false);
  const [nameCheckId, setNameCheckId] = useState<undefined | string>();

  const fields = useMemo(() => {
    return [
      {
        id: "name",
        label: "Name",
        value: integration_form.name || "",
        required: true,
        type: "text",
        visible: true
      },
      {
        id: "description",
        label: "Description",
        value: integration_form.description || "",
        required: false,
        type: "text",
        visible: true
      },
      {
        id: "url",
        label: "Url",
        value: integration_form.url || "",
        required: true,
        type: "text",
        visible: [IntegrationAuthTypes.PUBLIC_API_KEY, IntegrationAuthTypes.PRIVATE_API_KEY].includes(sub_type),
        info_url: "someurl.com"
      },
      {
        id: "username",
        label: "UserName",
        value: integration_form.username || "",
        required: true,
        type: "text",
        visible: [IntegrationAuthTypes.PRIVATE_API_KEY].includes(sub_type) && ["bitbucket"].includes(type)
      },
      {
        id: "apikey",
        label: "Api Key",
        value: integration_form.apikey || "",
        required: true,
        type: "password",
        visible: ![IntegrationAuthTypes.OAUTH].includes(sub_type),
        info_url: "someurl.com"
      }
    ];
  }, [integration_form]);

  const isFormValid = () => {
    const visibleNRequiredFields = fields.filter((field: any) => field.visible).filter((field: any) => field.required);
    let valid = visibleNRequiredFields.every(
      (field: any) => fieldTouched[field.id] && get(integration_form, [field.id], false)
    );
    valid = valid && !nameExist;
    onValidation(valid);
  };

  useEffect(() => {
    isFormValid();
  }, [fieldInfo, fieldTouched, integration_form]);

  useEffect(() => {
    if (nameChecking && nameCheckId) {
      const loading = get(integrationListState, [nameCheckId, "loading"], true);
      const error = get(integrationListState, [nameCheckId, "loading"], true);

      if (!loading && !error) {
        const data = get(integrationListState, [nameCheckId, "data", "records"], []);
        const checkName = checkTemplateNameExists(integration_form.name, data);
        setNameChecking(false);
        setNameCheckId(undefined);
        setNameExist(checkName);
      }
    }
  }, [integrationListState]);

  const fieldStatus = useCallback(
    (field: any) => {
      if (!field.required) {
        return SUCCESS;
      }
      if (field.id === "name") {
        if (nameChecking) {
          return VALIDATING;
        }
        if (!nameChecking && nameExist && fieldInfo[field.id]) {
          return ERROR;
        }
        if (!nameChecking && !nameExist && field.value !== "") {
          return SUCCESS;
        }
      }
      if (!get(integration_form, [field.id], false) && fieldInfo[field.id]) {
        return ERROR;
      }

      return undefined;
    },
    [integration_form, fieldInfo, nameChecking]
  );

  const handleChange = useCallback(
    (value: any) => {
      value = value || [];
      updateField("tags", value);
    },
    [updateField]
  );

  const handleInputChange = useCallback(
    (id: string) => {
      return (e: any) => {
        updateField(id, e.target.value);
        if (id === "name") {
          checkDebouncedName(e.target.value);
        }
      };
    },
    [updateField]
  );

  const handleMetadataValueChange = useCallback(
    (value: any) => {
      let metadata = get(integration_form, ["metadata"], {});
      metadata = {
        ...metadata,
        ...value
      };
      updateField("metadata", metadata);
    },
    [integration_form]
  );
  const checkName = (name: string) => {
    const filter = {
      partial: {
        name
      }
    };
    const id = uuidv1();
    dispatch(integrationsList({ filter }, null, id));
    setNameCheckId(id);
    setNameChecking(true);
  };

  const checkDebouncedName = useCallback(debounce(checkName, 500), []);

  const helpText = (field: any) => {
    if (field.id === "name" && nameExist && fieldInfo[field.id] && !nameChecking) {
      return "This integration name already exist";
    }

    if (field.required && fieldStatus(field) === ERROR && field.value === "") {
      return EMPTY_FIELD_WARNING;
    }
  };

  return (
    <div className="flex align-center justify-center" style={{ height: "100%" }}>
      <Form layout="vertical" style={{ maxWidth: width, minWidth: width }}>
        <div>
          {fields
            .filter((field: any) => field.visible)
            .map(field => {
              return (
                <Form.Item
                  label={field.label}
                  colon={false}
                  key={field.id}
                  required={field.required}
                  validateStatus={fieldStatus(field)}
                  hasFeedback={field.required}
                  help={helpText(field)}>
                  <Input
                    type={field.type}
                    value={field.value}
                    placeholder={field.label}
                    onBlur={() => setFieldInfo((state: any) => ({ ...state, [field.id]: true }))}
                    onFocus={() => setFieldTouched((state: any) => ({ ...state, [field.id]: true }))}
                    onChange={handleInputChange(field.id)}
                  />
                </Form.Item>
              );
            })}
          <Form.Item label={"Tags"} colon={false} key={"tags"}>
            <SelectRestapi
              value={integration_form.tags || []}
              mode={"multiple"}
              labelInValue={true}
              uri={"tags"}
              fetchData={tagsList}
              createOption={true}
              searchField="name"
              onChange={handleChange}
            />
          </Form.Item>
          {type === IntegrationTypes.BITBUCKET && (
            <Form.Item label={"REPOSITORIES"} colon={false} key={"bitbucket_repos"} help={BITBUCKET_REPO_DESC}>
              <CommaSelectTag
                onChange={(value: string) => handleMetadataValueChange({ repos: value })}
                value={integration_form?.metadata?.repos || ""}
              />
            </Form.Item>
          )}
          <IntegrationMetaFields
            updateMetaField={handleMetadataValueChange}
            applicationType={type}
            metadata={integration_form?.metadata || {}}
            satellite={integration_form?.satellite || false}
          />
        </div>
      </Form>
    </div>
  );
};
