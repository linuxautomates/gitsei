import { Icon } from "antd";
import moment from "moment";
import React, { useCallback, useMemo, useState } from "react";
import { AntButton, AntCheckbox, AntModal, AntSelect, AntTitle } from "shared-resources/components";
import { enrollmentPeriodOptions, ENROLLMENT_PERIOD_TEXT, SEND_EMAIL_TEXT } from "./global.constants";
import "./mfaSettings.scss";

interface MFASettingsModalProps {
  mfaEnrollmentPeriod?: number;
  onCancel: () => void;
  onSave: (timestamp: number, emailCheck?: boolean) => void;
  visible: boolean;
  globalSetting?: boolean;
  emailCheck?: boolean;
}

const MFASettingsModal: React.FC<MFASettingsModalProps> = (props: MFASettingsModalProps) => {
  const { mfaEnrollmentPeriod: initialPeriod, visible, onCancel, onSave } = props;
  const [mfaEnrollmentPeriod, setMfaEnrollmentPeriod] = useState<number | undefined>(initialPeriod);
  const [emailCheckbox, setEmailCheckbox] = useState<boolean>(props.emailCheck || true);

  const selectedOption = useMemo(() => {
    if (mfaEnrollmentPeriod) {
      const days = Math.abs(moment().startOf("d").diff(moment.unix(mfaEnrollmentPeriod).startOf("d"), "days"));
      if (enrollmentPeriodOptions.map((option: { label: string; value: number }) => option.value).includes(days)) {
        return days;
      }
      return 2;
    }
    return 2;
  }, [mfaEnrollmentPeriod]);

  const handleTimePeriodChange = useCallback((value: number) => {
    setMfaEnrollmentPeriod(moment().add(value, "d").endOf("d").unix());
  }, []);

  const handleOnSave = useCallback(() => {
    onSave(moment().add(selectedOption, "d").endOf("d").unix(), emailCheckbox);
  }, [selectedOption, emailCheckbox]);

  const handleEmailCheckboxChange = useCallback(event => {
    setEmailCheckbox(event.target.checked);
  }, []);

  return (
    <AntModal
      visible={visible}
      closable={false}
      wrapClassName={"mfa-settings-modal-wrapper"}
      onOk={handleOnSave}
      onCancel={onCancel}
      footer={
        <div className={"flex justify-end mt-20"}>
          <AntButton type={"ghost"} onClick={onCancel}>
            Cancel
          </AntButton>
          <AntButton type={"primary"} onClick={handleOnSave}>
            Save
          </AntButton>
        </div>
      }
      title={
        <div className={"flex justify-space-between mb-20"}>
          <span>Enable MFA</span>
          <Icon onClick={onCancel} style={{ color: "#8C8C8C" }} type="close" />
        </div>
      }>
      <div className="mfa-settings-container">
        <div className="mfa-select">
          <AntTitle className="enrollment-title">{ENROLLMENT_PERIOD_TEXT}</AntTitle>
          <AntSelect
            className={"w-100"}
            showArrow
            mode="single"
            options={enrollmentPeriodOptions}
            value={selectedOption}
            onChange={handleTimePeriodChange}
          />
          <p className="enrollment-period-text">
            Note: Enrollment period provides grace period for users to activate MFA. Contributors who don't enable MFA
            during the enrollment period will get locked out.
          </p>
        </div>
        {props.globalSetting && (
          <AntCheckbox checked={emailCheckbox} onChange={handleEmailCheckboxChange}>
            {SEND_EMAIL_TEXT}
          </AntCheckbox>
        )}
      </div>
    </AntModal>
  );
};

export default React.memo(MFASettingsModal);
