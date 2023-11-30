import React from "react";
import { connect } from "react-redux";
import { Icon } from "antd";

import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import LocalStoreService from "services/localStoreService";
// @ts-ignore
import { AntButton, AntText, AvatarWithText } from "shared-resources/components";
import "./assignee-select.styles.scss";
import SlideDown from "react-slidedown";
import Loader from "components/Loader/Loader";
import { validateEmail } from "utils/stringUtils";
import AssigneeModalSheet from "./assigneeModalSheet";
import UserEditForm from "configurations/components/UserEditForm/UserEditForm";

export interface AssigneeSelectComponentProps {
  placeholder?: string;
  assignees: any[];
  onAssigneesChange: (assignees: { key: string; label: string }[]) => void;
  rest_api?: object;
  usersList: () => {};
  showAssignItToMeAlways?: boolean;
  user_form: any;
  formUpdateField: (name: string, field: string, value: string | boolean) => void;
  usersCreate: (user: any) => void;
  showCreateAssignee: boolean;
  handleCreateAssigneeCancel: () => void;
  handleCreateAssigneeSubmit: () => void;
  createAssigneeLoading: boolean;
  updateBtnStatus: boolean;
  setUpdateBtnStatus: (value: boolean) => void;
  newUserSelectionId: string;
  refreshList: number;
  allowCreateAssignee: boolean;
}

class AssigneeSelectComponent extends React.Component<AssigneeSelectComponentProps> {
  selectRef: React.RefObject<any>;

  constructor(props: AssigneeSelectComponentProps) {
    super(props);
    this.state = {
      show_dropdown: false
    };
    this.selectRef = React.createRef();
  }

  get loggedInUserId() {
    const localStorage = new LocalStoreService();
    return localStorage.getUserId();
  }

  get loggedInUserEmail() {
    const localStorage = new LocalStoreService();
    return localStorage.getUserEmail();
  }

  handleAssignItToMe = () => {
    if (this.isAssignedToMe) {
      return;
    }
    let options = this.assignees;
    if (!options) {
      options = [];
    }
    options.push({
      key: this.loggedInUserId as any,
      label: this.loggedInUserEmail as any
    });
    this.props.onAssigneesChange(options);
  };

  handleUnassigned = () => {
    this.props.onAssigneesChange([]);
  };

  handleSelectBlur = () => {
    this.setState({
      show_dropdown: false
    });
  };

  handleRemove = (item: { key: string; label: string }) => {
    if (!this.assignees) {
      return;
    }
    this.props.onAssigneesChange(this.assignees.filter((v: { key: string; label: string }) => v.key !== item.key));
  };

  handleAdd = () => {
    this.setState(
      {
        show_dropdown: true
      },
      () => {
        this.selectRef.current && this.selectRef.current.focus();
      }
    );
  };

  get showDropdown() {
    // @ts-ignore
    return this.state.show_dropdown;
  }

  get assignees(): { key: string; label: string }[] | null {
    return this.props.assignees || null;
  }

  get isAssignedToMe() {
    return this.assignees && !!this.assignees.filter(assignee => assignee.key === this.loggedInUserId).length;
  }

  get showAssignItToMe() {
    return (this.loggedInUserId && this.loggedInUserEmail && !this.isAssignedToMe) || this.props.showAssignItToMeAlways;
  }

  get showUnassign() {
    return this.assignees && !!this.assignees.length;
  }

  get assignItToMeButton() {
    if (!this.showAssignItToMe) {
      return null;
    }
    return (
      <div onClick={this.handleAssignItToMe} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        Assign it to me
      </div>
    );
  }

  get unassignedButton() {
    if (!this.showUnassign) {
      return null;
    }
    return (
      <div onClick={this.handleUnassigned} className="add-btn">
        <Icon className="add-icon" type="close-circle" />
        Unassigned
      </div>
    );
  }

  get addAssigneeBtn() {
    return (
      <div onClick={this.handleAdd} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        Add assignee
      </div>
    );
  }

  get createAssigneeButton() {
    return (
      <div onClick={this.props.handleCreateAssigneeCancel} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        Create assignee
      </div>
    );
  }

  get actionButtons() {
    return (
      <div className="actions">
        {this.addAssigneeBtn}
        {this.assignItToMeButton}
        {this.unassignedButton}
        {/*{this.props.allowCreateAssignee && this.createAssigneeButton}*/}
      </div>
    );
  }

  get select() {
    return (
      <AssigneeModalSheet
        className="assignee-modal-sheet"
        visible={this.showDropdown}
        onCancel={() => {
          this.setState({ show_dropdown: false });
        }}
        selectedValue={this.assignees}
        onSave={(assignees: any) => {
          this.props.onAssigneesChange(assignees);
          this.setState({ show_dropdown: false });
        }}
      />
    );
  }

  get renderAssignes() {
    if (!this.assignees) {
      return null;
    }
    return this.assignees.map((v: any) => (
      <div className="selected-option">
        <AvatarWithText className="content" key={v.key} avatarText={v.label} text={v.label} showTooltip />
        <Icon
          className="close-icon"
          type="close"
          onClick={() => {
            this.handleRemove(v);
          }}
        />
      </div>
    ));
  }

  get createAssignee() {
    return (
      <SlideDown>
        {
          // @ts-ignore
          this.props.showCreateAssignee ? (
            <div
              style={{
                marginBottom: "1rem",
                border: "1px solid #ddd",
                borderRadius: "10px",
                padding: "1rem",
                backgroundColor: "#f3f3f3"
              }}>
              {this.props.createAssigneeLoading && <Loader />}
              {!this.props.createAssigneeLoading && (
                <>
                  <AntText style={{ fontSize: "14px", marginBottom: "8px", display: "block" }}>
                    Create new user to automatically assign them
                  </AntText>
                  <UserEditForm
                    user_form={this.props.user_form}
                    // @ts-ignore
                    formUpdateField={this.props.formUpdateField}
                    compact={true}
                  />
                  <div style={{ width: "fit-content", marginLeft: "auto", marginBottom: "1.5rem" }}>
                    <AntButton
                      type="primary"
                      onClick={this.props.handleCreateAssigneeSubmit}
                      disabled={
                        !this.props.user_form.email.length ||
                        //!this.props.user_form.first_name.length ||
                        //!this.props.user_form.last_name.length ||
                        !validateEmail(this.props.user_form.email)
                      }>
                      Create
                    </AntButton>
                    <AntButton style={{ marginLeft: "1rem" }} onClick={this.props.handleCreateAssigneeCancel}>
                      Cancel
                    </AntButton>
                  </div>
                </>
              )}
            </div>
          ) : null
        }
      </SlideDown>
    );
  }

  render() {
    return (
      <div className="assignee-select">
        {this.renderAssignes}
        {this.actionButtons}
        {this.props.allowCreateAssignee && this.createAssignee}
        {this.select}
      </div>
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(AssigneeSelectComponent);
