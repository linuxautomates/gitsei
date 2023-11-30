import React, { useMemo, useState } from "react";
import { AntButton, AntModal, AntTable, AntText } from "../../../../shared-resources/components";

interface CsvUploadWrapperProps {
  width?: string;
  visible: boolean;
  handleClose: (visible: boolean) => void;
  steps: any[];
}

export const CsvUploadWrapper: React.FC<CsvUploadWrapperProps> = ({ visible, handleClose, steps, width }) => {
  const [currentStep, setCurrentStep] = useState<number>(0);
  const handleCancel = () => {
    const stepCloseHandler = steps[currentStep]?.closeHandler;
    stepCloseHandler && stepCloseHandler();
    handleClose(false);
    setCurrentStep(0);
  };

  const nextHandler = () => {
    setCurrentStep(state => state + 1);
  };

  const closeHandler = () => {};

  const backHandler = () => {
    const stepBackHandler = steps[currentStep]?.backhandler;
    stepBackHandler && stepBackHandler();
    setCurrentStep(state => state - 1);
  };

  const nextButton = useMemo(() => {
    return (
      <AntButton type={"primary"} onClick={nextHandler}>
        Next
      </AntButton>
    );
  }, []);

  const backbutton = useMemo(() => {
    return (
      <AntButton type={"primary"} onClick={backHandler}>
        Back
      </AntButton>
    );
  }, [currentStep]);

  const closebutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={handleCancel}>
        Close
      </AntButton>
    );
  }, [currentStep, handleCancel]);

  const cancelbutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={handleCancel}>
        Close
      </AntButton>
    );
  }, [handleCancel]);

  const buttonMapping: any = {
    next: nextButton,
    close: closebutton,
    cancel: cancelbutton,
    back: backbutton
  };

  return (
    <AntModal
      title={steps[currentStep].title}
      visible={visible}
      onCancel={handleCancel}
      width={width ? width : "80vw"}
      footer={
        <>
          {buttonMapping[steps[currentStep].footer[0]]}
          {buttonMapping[steps[currentStep].footer[1]]}
        </>
      }>
      {steps[currentStep].renderContent}
    </AntModal>
  );
};

export default CsvUploadWrapper;
