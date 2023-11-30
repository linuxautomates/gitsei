import React, { Component } from "react";
import * as PropTypes from "prop-types";
import { AntCard, AntCheckbox, AntCol, AntRow } from "shared-resources/components";
import { isUndefined } from "lodash";

export class DefaultFieldComponent extends Component {
  constructor(props) {
    super(props);

    this.onChange = this.onChange.bind(this);
  }

  onChange(e) {
    let default_fields = {
      summary: true,
      assignee: true,
      description: isUndefined((this.props.data || {}).description) ? true : this.props.data.description,
      tags: isUndefined((this.props.data || {}).tags) ? true : this.props.data.tags,
      type: isUndefined((this.props.data || {}).type) ? true : this.props.data.type,
      attachments: isUndefined((this.props.data || {}).attachments) ? true : this.props.data.attachments
    };

    switch (e.target.value) {
      case "description_checked":
        default_fields = {
          ...default_fields,
          description: e.target.checked
        };
        this.props.onUpdateValues(default_fields);
        break;
      case "tags_checked":
        default_fields = {
          ...default_fields,
          tags: e.target.checked
        };
        this.props.onUpdateValues(default_fields);
        break;
      case "type_checked":
        default_fields = {
          ...default_fields,
          type: e.target.checked
        };
        this.props.onUpdateValues(default_fields);
        break;
      case "attachments_checked":
        default_fields = {
          ...default_fields,
          attachments: e.target.checked
        };
        this.props.onUpdateValues(default_fields);
        break;
      default:
    }
  }

  render() {
    const options = [
      { label: "Summary", value: "summary", disabled: true, checked: true },
      { label: "Assignee", value: "assignee", disabled: true, checked: true },
      {
        label: "Description",
        value: "description_checked",
        disabled: false,
        checked: isUndefined((this.props.data || {}).description) ? true : this.props.data.description
      },
      {
        label: "Tags",
        value: "tags_checked",
        disabled: false,
        checked: isUndefined((this.props.data || {}).tags) ? true : this.props.data.tags
      },
      {
        label: "Type",
        value: "type_checked",
        disabled: false,
        checked: isUndefined((this.props.data || {}).type) ? true : this.props.data.type
      },
      {
        label: "Attachments",
        value: "attachments_checked",
        disabled: false,
        checked: isUndefined((this.props.data || {}).attachments) ? true : this.props.data.attachments
      }
    ];
    return (
      <AntCol span={16}>
        <AntCard title={"Default Metadata Fields"}>
          <div style={{ height: "190px" }}>
            {options.map(option => {
              return (
                <AntRow type={"flex"} justify={"start"} gutter={[20, 20]}>
                  <AntCol span={8}>
                    <AntCheckbox
                      value={option.value}
                      checked={option.checked}
                      onChange={this.onChange}
                      disabled={option.disabled}>
                      {option.label}
                    </AntCheckbox>
                  </AntCol>
                </AntRow>
              );
            })}
          </div>
        </AntCard>
      </AntCol>
    );
  }
}

DefaultFieldComponent.propTypes = {
  onUpdateValues: PropTypes.func.isRequired
};
