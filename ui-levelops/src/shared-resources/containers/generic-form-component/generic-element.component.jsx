import { Form } from "antd";
import { FieldTypes } from "classes/FieldTypes";
import { DatePickerWrapper } from "../generic-form-elements/date-picker/date-picker.component";
import { FileUploadWrapper } from "../generic-form-elements/file-upload/file-upload.component";
import { DynamicMultiSelectWrapper } from "../generic-form-elements/dynamic-select/dynamic-multi-select.component";
import { DynamicSelectComponent } from "../generic-form-elements/dynamic-select/dynamic-select.component";
import { DynamicCustomMultiSelectWrapper } from "../generic-form-elements/dynamic-select/dynamic-custom-multi-select.component";
import { DynamicCustomSelectComponent } from "../generic-form-elements/dynamic-select/dynamic-custom-select.component";
import { RadioGroupWrapper } from "../generic-form-elements/radio-group/radio-group.component";
import { SelectComponent } from "../generic-form-elements/select/select.component";
import { MultiSelectWrapper } from "../generic-form-elements/select/multi-select.component";
import { TextWrapper } from "../generic-form-elements/text/text.component";
import { PasswordTextWrapper } from "../generic-form-elements/text/password-text.component";
import { TextAreaWrapper } from "../generic-form-elements/text-area/text-area.component";
import { VariableSingleSelectWrapper } from "../generic-form-elements/select/variable-select.component";
import { default as AssessmentCheck } from "../generic-form-elements/assessment-check/assessment-check.component";
import { KVComponent } from "../generic-form-elements/kv-component/kv.component";
import { CheckboxGroupWrapper } from "../generic-form-elements/checkbox-group/checkbox-group.component";
import { default as CustomFieldsComponent } from "../generic-form-elements/custom-fields/custom-fields.component";
import { default as CustomFieldsComponentNew } from "../generic-form-elements/custom-fields/custom-fields-new.component";
import { ConfigTableFilterComponent } from "../generic-form-elements/config-table-filter/config-table-filter.component";
import { ConfigTableColumnComponent } from "../generic-form-elements/config-table-column/config-table-column.component";
import { default as CodeEditorWrapper } from "../generic-form-elements/code-editor/CodeEditorWrapper";
import { default as JiraCustomSelectWrapper } from "../generic-form-elements/jira-multi-select-fields/JiraFiltersMultiSelect";
import { default as InputTagsComponent } from "../generic-form-elements/input-tags/input-tags.component";
import { default as CommaSelectTag } from "../generic-form-elements/select/comma-multi-select-tag";
import envConfig from "env-config";
import {
  ASSESSMENT_CHECK_TYPE,
  AUTOSUGGEST_TYPE,
  CHECKBOX_GROUP_TYPE,
  COMMA_MULTI_SELECT,
  CONFIG_TABLE_COLUMN_TYPE,
  CONIFG_TABLE_FILTER_TYPE,
  CUSTOM_FIELDS_TYPE,
  DATE,
  DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
  DYNAMIC_MULTI_SELECT_TYPE,
  DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  FILE_UPLOAD_TYPE,
  INPUT_TAGS_TYPE,
  JIRA_CUSTOM_SELECT_TYPE,
  KV_TYPE,
  LQL_TYPE,
  MULTI_SELECT_TYPE,
  RADIO_GROUP_TYPE,
  SINGLE_SELECT_TYPE,
  TEXT_AREA_TYPE,
  TEXT_EDITOR,
  TEXT_TYPE,
  VARIABLE_SELECT_TYPE,
  PASSWORD_STRING
} from "constants/fieldTypes";
import React from "react";
import { AutosuggestContainer } from "shared-resources/containers/autosuggest/autosuggest.container";
import { LQLContainer } from "shared-resources/containers/lql/lql.container";

const Components = {
  [DATE]: DatePickerWrapper,
  [TEXT_AREA_TYPE]: TextAreaWrapper,
  [TEXT_TYPE]: TextWrapper,
  [SINGLE_SELECT_TYPE]: SelectComponent,
  [MULTI_SELECT_TYPE]: MultiSelectWrapper,
  [DYNAMIC_SINGLE_SELECT_TYPE]: DynamicSelectComponent,
  [DYNAMIC_MULTI_SELECT_TYPE]: DynamicMultiSelectWrapper,
  [RADIO_GROUP_TYPE]: RadioGroupWrapper,
  [FILE_UPLOAD_TYPE]: FileUploadWrapper,
  [LQL_TYPE]: LQLContainer,
  [AUTOSUGGEST_TYPE]: AutosuggestContainer,
  [ASSESSMENT_CHECK_TYPE]: AssessmentCheck,
  [KV_TYPE]: KVComponent,
  [VARIABLE_SELECT_TYPE]: VariableSingleSelectWrapper,
  [CHECKBOX_GROUP_TYPE]: CheckboxGroupWrapper,
  [DYNAMIC_MULTI_CUSTOM_SELECT_TYPE]: DynamicCustomMultiSelectWrapper,
  [DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE]: DynamicCustomSelectComponent,
  [CUSTOM_FIELDS_TYPE]:
    envConfig.get("PROPEL_SPLIT_JIRA_CUSTOM_FIELD_INTEGRATION") === "show"
      ? CustomFieldsComponentNew
      : CustomFieldsComponent,
  [CONIFG_TABLE_FILTER_TYPE]: ConfigTableFilterComponent,
  [CONFIG_TABLE_COLUMN_TYPE]: ConfigTableColumnComponent,
  [TEXT_EDITOR]: CodeEditorWrapper,
  [JIRA_CUSTOM_SELECT_TYPE]: JiraCustomSelectWrapper,
  [INPUT_TAGS_TYPE]: InputTagsComponent,
  [COMMA_MULTI_SELECT]: CommaSelectTag,
  [PASSWORD_STRING]: PasswordTextWrapper
};

const TriggerNodeComponents = {
  ...Components,
  [CUSTOM_FIELDS_TYPE]: CustomFieldsComponent
};

export class GenericElementComponent extends React.Component {
  get element() {
    return this.props.element;
  }

  get value() {
    // value is always an array. Things that are string value, treat it as singleton array and return the first row
    const { element } = this.props;
    if (
      ![DYNAMIC_MULTI_SELECT_TYPE, MULTI_SELECT_TYPE, KV_TYPE, DYNAMIC_MULTI_CUSTOM_SELECT_TYPE].includes(element.type)
    ) {
      if (Array.isArray(this.props.value)) {
        return this.props.value[0];
      }
    }
    return this.props.value;
  }

  get options() {
    return this.element.options;
  }

  get validate() {
    const { validation, validation_props } = this.element;
    const validator = FieldTypes[validation];
    if (!validator) {
      return {
        validateStatus: null,
        errorMessage: null
      };
    }
    if (validation_props) {
      return validator(this.value, validation_props);
    }
    return validator(this.value);
  }

  handleChange = value => {
    const { element } = this.props;
    if (
      ![
        DYNAMIC_MULTI_SELECT_TYPE,
        MULTI_SELECT_TYPE,
        KV_TYPE,
        DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
        COMMA_MULTI_SELECT
      ].includes(element.type)
    ) {
      this.props.onChange(element.key, [value]);
      return;
    }
    this.props.onChange(element.key, value);
  };

  handleLQLChange = (query, result, id) => {
    const { element } = this.props;
    this.props.onChange(element.key, [query]);
  };

  render() {
    const { element, triggerNode } = this.props;
    const component = triggerNode ? TriggerNodeComponents[element.type] : Components[element.type];
    if (!component) {
      return null;
    }
    const { validateStatus, errorMessage } = this.validate;
    const componentValue = this.value;
    return (
      <Form.Item
        label={element.label}
        colon={false}
        extra={element.description || ""}
        required={!!element.required}
        validateStatus={validateStatus}
        help={errorMessage}>
        {React.createElement(component, {
          ...element,
          onChange: element.type === "lql" ? this.handleLQLChange : this.handleChange,
          value:
            componentValue ||
            (element.type === CUSTOM_FIELDS_TYPE && componentValue === undefined
              ? {
                  integration_id: undefined,
                  custom_fields: []
                }
              : componentValue)
        })}
      </Form.Item>
    );
  }
}
