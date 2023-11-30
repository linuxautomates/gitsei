import React from "react";
import { AntModal } from "../../../../shared-resources/components/index";
import "./error-details-modal.container.scss";
interface ErrorDetailsModalContainerProps {
  visible: boolean;
  onClose: () => void;
  message: string;
  stacktrace: [];
}

const ErrorDetailsModalContainer: React.FC<ErrorDetailsModalContainerProps> = ({
  visible,
  onClose,
  message,
  stacktrace
}) => {
  let stacktraceContainer;

  /**
   * We'll only display the most useful message for the user, which
   * is the first line of the last stacktrace from the data
   */
  if (stacktrace.length > 0) {
    try {
      stacktraceContainer = (
        <div className="stacktrace-container">
          <div className="details">
            <div className="stacktrace-line">{Object.keys(stacktrace[stacktrace.length - 1])[0]}</div>
          </div>
        </div>
      );
    } catch (err) {
      stacktraceContainer = <>No additional information available</>;
    }
  } else {
    stacktraceContainer = <>No additional information available</>;
  }

  return (
    <AntModal
      className="error-details-modal"
      visible={visible}
      footer={null}
      closable
      onCancel={onClose}
      centered
      title={"Error details"}>
      <div className="stacktraces-container">{stacktraceContainer}</div>
    </AntModal>
  );
};

export default React.memo(ErrorDetailsModalContainer);
