import React, { useState } from "react";
import { useDispatch } from "react-redux";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import LocalStoreService from "services/localStoreService";
import { AntButton, AntModal, AntForm, AntFormItem } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { RestKBSend } from "classes/RestKB";
import { RestCommTemplate } from "classes/RestCommTemplate";
import { validateEmail } from "utils/stringUtils";
import { Select, Input } from "antd";
import { EMAIL_WARNING } from "../../constants/formWarnings";
import { debounce } from "lodash";
import { bpsSend, bpsList, usersList, cTemplatesList } from "reduxConfigs/actions/restapi";

const { Option } = Select;

interface SendKBContainerProps {
  workItemid: string;
  visible: boolean;
  artifact: string;
  onCancel: () => void;
}

const SendKBContainer: React.FC<SendKBContainerProps> = props => {
  const dispatch = useDispatch();

  const [bps_select, setbpsSelect] = useState<any>({});
  const [bps_to, setbpsTo] = useState<any>({});
  const [additional_info, setAdditionalInfo] = useState("");
  const [template_select, setTemplateSelect] = useState<any>({});
  const [template_type_select, setTemplateTypeSelect] = useState<any>({});

  const resetForm = () => {
    setbpsSelect({});
    setbpsTo({});
    setAdditionalInfo("");
    setTemplateSelect({});
    setTemplateTypeSelect({});
  };

  const footer = () => {
    return (
      <div className={`flex direction-row justify-end`}>
        <AntButton
          onClick={() => {
            resetForm();
            props.onCancel();
          }}>
          Cancel
        </AntButton>
        <AntButton
          type="primary"
          disabled={
            // @ts-ignore
            bps_select === {} ||
            bps_to === "" ||
            !validateEmail(bps_to.label) ||
            template_type_select.key === undefined ||
            template_select.key === undefined
          }
          onClick={() => {
            let createQ = new RestKBSend();
            let ls = new LocalStoreService();
            createQ.best_practices_id = bps_select.key;
            createQ.sender_email = ls.getUserEmail();
            createQ.target_email = bps_to.label;
            createQ.comm_template_id = template_select.key;
            createQ.work_item_id = props.workItemid;
            createQ.additional_info = additional_info;
            createQ.artifact = props.artifact;

            dispatch(bpsSend(createQ));
            debounce(props.onCancel, 100)();
            resetForm();
          }}>
          Send
        </AntButton>
      </div>
    );
  };

  return (
    <AntModal
      visible={props.visible}
      title="Send Knowledge Base"
      footer={footer()}
      width={640}
      onCancel={() => {
        resetForm();
        props.onCancel();
      }}>
      <AntForm layout="vertical">
        <AntFormItem required label="Knowledge Base">
          <SelectRestapi
            searchField="name"
            uri="bestpractices"
            fetchData={bpsList}
            method="list"
            //rest_api={this.props.rest_api}
            isMulti={false}
            closeMenuOnSelect={true}
            labelInValue={true}
            value={bps_select}
            creatable={false}
            mode={"single"}
            onChange={(option: any) => {
              setbpsSelect(option || {});
            }}
          />
        </AntFormItem>
        <AntFormItem required label="To" hasFeedback help={validateEmail(bps_to.label) ? "" : EMAIL_WARNING}>
          <SelectRestapi
            searchField="email"
            uri="users"
            fetchData={usersList}
            method="list"
            isMulti={false}
            closeMenuOnSelect={true}
            value={bps_to}
            createOption={true}
            labelInValue={true}
            mode={"single"}
            onChange={(option: any) => {
              if (option && Array.isArray(option.label)) {
                option.label = option.label[1];
              }
              setbpsTo(option || {});
            }}
          />
        </AntFormItem>
        <AntFormItem required label="Template Type">
          <Select
            // @ts-ignore
            options={RestCommTemplate.OPTIONS.map(type => ({ label: type, key: type }))}
            value={template_type_select}
            labelInValue={true}
            onChange={option => {
              setTemplateTypeSelect(option);
              setTemplateSelect({});
            }}>
            {RestCommTemplate.OPTIONS.map(type => (
              <Option key={type}>{type}</Option>
            ))}
          </Select>
        </AntFormItem>
        <AntFormItem
          required
          label="Template"
          help="Selected template will be added as message with questionnaire link">
          <SelectRestapi
            disabled={template_type_select.key === undefined}
            searchField="name"
            moreFilters={{ type: template_type_select.key }}
            uri="ctemplates"
            fetchData={cTemplatesList}
            method="list"
            //rest_api={this.props.rest_api}
            isMulti={false}
            closeMenuOnSelect={true}
            value={template_select}
            creatable={false}
            mode={"single"}
            labelInValue={true}
            onChange={(option: any) => {
              setTemplateSelect(option);
            }}
          />
        </AntFormItem>
        <AntFormItem label="Additional Information">
          <Input.TextArea
            autoSize={{ minRows: 2, maxRows: 4 }}
            id="additional-info"
            value={additional_info}
            onChange={e => setAdditionalInfo(e.target.value)}
          />
        </AntFormItem>
      </AntForm>
    </AntModal>
  );
};

export default SendKBContainer;
