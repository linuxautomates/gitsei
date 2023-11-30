import React from "react";
import { Prompt } from "react-router-dom";
import { ConfirmationModal } from "shared-resources/components";
interface State {
  dirty: boolean;
  currentLocation: string;
  hardRedirect: boolean;
  visibleModal: boolean;
  nextLocation: any;
  action: any;
  saveInProgress: boolean;
}

export interface ConfirmationWrapperProps {
  setDirty: (value: boolean) => void;
  setHardRedirect?: (value: boolean) => void;
  bindSaveAction?: (saveAction: (...args: any) => any) => any;

  // error indicates that the save failed.
  onSaveActionComplete?: (error: boolean) => any;
}

const WARNING_MESSAGE = "Warning: Page has unsaved changes that will be lost. Would you like to stay on the page?";
const SAVE_MODE_WARNING_MESSAGE = "There are unsaved changes. What would you like to do?";

const shouldUseSaveMode = (path: string) => {
  if (path.includes("propels-editor") || path.includes("dashboards/create")) {
    return true;
  }

  return false;
};

function ConfirmationWrapper(WrappedComponent: React.ComponentType<any>) {
  return class extends React.Component<any, State> {
    saveAction: ((...args: any) => any) | undefined;

    constructor(props: any) {
      super(props);

      this.state = {
        dirty: false,
        currentLocation: "",
        hardRedirect: false,
        visibleModal: false,
        nextLocation: undefined,
        action: undefined,
        saveInProgress: false
      };

      this.saveAction = undefined;
      this.setDirty = this.setDirty.bind(this);
      this.setHardRedirect = this.setHardRedirect.bind(this);
    }

    componentDidMount() {
      this.setState({ currentLocation: window.location.href });
      window.onbeforeunload = (ev: any) => {
        ev.preventDefault?.();
        if (!this.state.dirty) {
          return;
        }
        return WARNING_MESSAGE;
      };
    }

    componentWillUnmount() {
      this.setDirty(false);
    }

    setDirty(state: boolean) {
      this.setState({ dirty: state });
    }

    setHardRedirect(value: boolean) {
      this.setState({ hardRedirect: value });
    }

    handleOnOk = () => {
      const saveMode = shouldUseSaveMode(this.state.currentLocation);

      if (saveMode && this.saveAction) {
        this.setState({ saveInProgress: true });
        this.saveAction();
      } else {
        this.stayOnPage();
      }
    };

    stayOnPage = () => {
      const location = this.state.nextLocation;
      this.setState({ dirty: false, visibleModal: false }, () => {
        if (this.state.action !== "PUSH") {
          this.props.history?.push?.(`${location?.pathname}${location?.search}`);
        }
        this.props.history?.push?.(this.state.currentLocation.split("#")[1]);
        this.setState({ dirty: true });
      });
    };

    leavePage = () => {
      const location = this.state.nextLocation;
      this.setState({ dirty: false, visibleModal: false }, () => {
        const path = `${location?.pathname}${location?.search}`;
        if (this.state.hardRedirect) {
          window.location.href = path;
          window.location.reload();
        } else {
          this.props.history?.replace?.(path);
        }
      });
    };

    handleOnCancel = (e: any) => {
      this.stayOnPage();
    };

    handleOnReject = () => {
      this.leavePage();
    };

    onSaveActionComplete = (error?: boolean) => {
      if (this.state.saveInProgress) {
        if (error) {
          this.stayOnPage();
        } else {
          this.leavePage();
        }
      } else {
        // Not doing anything because the save
        // occurred apart from the 'unsaved changes'
        // flow.
      }

      this.setState({ saveInProgress: false });
    };

    bindSaveAction = (func: (...args: any) => any) => {
      this.saveAction = func;
    };

    render() {
      const saveMode = shouldUseSaveMode(this.state.currentLocation);

      return (
        <>
          <WrappedComponent
            {...this.props}
            setDirty={this.setDirty}
            setHardRedirect={this.setHardRedirect}
            bindSaveAction={this.bindSaveAction}
            onSaveActionComplete={this.onSaveActionComplete}
          />
          {this.state.visibleModal && (
            <ConfirmationModal
              text={saveMode ? SAVE_MODE_WARNING_MESSAGE : WARNING_MESSAGE}
              onCancel={this.handleOnCancel}
              onReject={this.handleOnReject}
              onOk={this.handleOnOk}
              visiblity={this.state.visibleModal}
              saveMode={saveMode}
            />
          )}
          <Prompt
            when={this.state.dirty}
            message={(location, action) => {
              this.setState({ visibleModal: true, nextLocation: location, action });
              return false;
            }}
          />
        </>
      );
    }
  };
}

export default ConfirmationWrapper;
