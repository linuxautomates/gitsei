import { Alert, Divider, Form, Input, notification, Result } from "antd";
import { MFA_CODE_INVALID } from "constants/formWarnings";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  sessionMFAEnroll,
  sessionMFAEnrollClear,
  sessionMFAEnrollGet,
  sessionMFAEnrollPost
} from "reduxConfigs/actions/restapi/mfa.action";
import { sessionMFAEnrollSelector } from "reduxConfigs/selectors/session_mfa.selector";
import {
  RestMFAEnrollGetState,
  RestMFAEnrollPostState,
  RestMFAEnrollState
} from "reduxConfigs/types/rest-state/session-reducer/RestMFAEnrollState";
import { AntButton, AntModal } from "shared-resources/components";
import { checkMFAValidation } from "utils/regexUtils";
import { formWarming } from "views/Pages/common/commons.component";
import "./mfa.styles.scss";
import {
  AFTER_SCAN_TEXT,
  ERROR_TEXT,
  FINISH_SETUP_TEXT,
  INPUT_1_LABEL,
  QR_HELP_TEXT,
  RESULT_FOOTER_TEXT,
  RESULT_TEXT,
  SETUP_MFA_MODAL_TEXT,
  SETUP_MODAL_HEADER_TEXT,
  SETUP_MODAL_SUB_HEADER_TEXT
} from "./MFASetup.constants";

interface MFASetupModalProps {
  showSetupModal: boolean;
  closeSetupModal: () => void;
  onMFAEnrollSuccess?: () => void;
}

const HAS_ERROR = "hasError";

const MFASetupModal: React.FC<MFASetupModalProps> = ({ showSetupModal, closeSetupModal, onMFAEnrollSuccess }) => {
  const [showResult, setShowResult] = useState<boolean>(false);
  const [code1, setCode1] = useState<string>();
  const [codeValidation, setCodeValidation] = useState<string>();

  const dispatch = useDispatch();
  const restSessionMFAEnroll: RestMFAEnrollState = useSelector(sessionMFAEnrollSelector);

  const restSessionEnrollGet: RestMFAEnrollGetState = useMemo(() => restSessionMFAEnroll?.get, [restSessionMFAEnroll]);
  const restSessionEnrollPost: RestMFAEnrollPostState = useMemo(
    () => restSessionMFAEnroll?.post,
    [restSessionMFAEnroll]
  );

  const mfaCodeWarning = useMemo(() => formWarming(MFA_CODE_INVALID), []);

  useEffect(() => {
    dispatch(sessionMFAEnrollClear());
    dispatch(sessionMFAEnrollGet());
  }, []);

  useEffect(() => {
    if (restSessionEnrollPost) {
      const { loading, error, enrollment_success } = restSessionEnrollPost;
      if (!loading && !error && enrollment_success) {
        onMFAEnrollSuccess?.();
        setShowResult(true);
      }
    }
  }, [restSessionEnrollPost]);

  useEffect(() => {
    return () => {
      dispatch(sessionMFAEnrollClear());
    };
  }, []);

  const handleCode1Change = useCallback(event => {
    setCode1(event.target.value);
    setCodeValidation(checkMFAValidation(event?.target?.value) ? "" : HAS_ERROR);
  }, []);

  const handleSubmit = useCallback(() => {
    if (showResult) {
      closeSetupModal();
    } else if ((code1 || "").length === 0) {
      notification.error({ message: "Code is required" });
    } else if (!checkMFAValidation(code1)) {
      notification.error({ message: MFA_CODE_INVALID });
    } else if (code1 && checkMFAValidation(code1)) {
      dispatch(sessionMFAEnroll("post", { loading: false, error: false }));
      dispatch(sessionMFAEnrollPost({ otp: code1 }));
    }
  }, [showResult, code1]);

  const renderSetupModalFooter = useMemo(() => {
    return (
      <div className="flex justify-center setup_modal_footer">
        {!showResult && (
          <AntButton onClick={closeSetupModal} type="ghost">
            Cancel
          </AntButton>
        )}
        <AntButton disabled={showResult ? false : !checkMFAValidation(code1)} onClick={handleSubmit} type="primary">
          {!!showResult ? RESULT_FOOTER_TEXT : FINISH_SETUP_TEXT}
        </AntButton>
      </div>
    );
  }, [showResult, handleSubmit, code1]);

  const renderResult = () => {
    return <Result status="success" title={RESULT_TEXT} />;
  };

  const renderOriginalContent = () => {
    return (
      <div className="setup-modal">
        <p className="setup-modal_header_text">{SETUP_MODAL_HEADER_TEXT}</p>
        <div className="setup-modal_subheader_container">
          <p className="text">{SETUP_MODAL_SUB_HEADER_TEXT}</p>
        </div>
        <div className="qrcode-container">
          <div className="qr-container">
            <img src={restSessionEnrollGet?.qrcode} alt="QR" className="qr-image" />
          </div>
        </div>
        <p className="qr-help-text">{QR_HELP_TEXT}</p>
        <div className="help-code-container">
          <div className="help-code">
            <p className="text">{restSessionEnrollGet?.code}</p>
          </div>
        </div>
        <Divider className="after-scan-divider" />
        <p>{AFTER_SCAN_TEXT}</p>
        <div className="verfication-code-container">
          <div className="input_one_container">
            <Form.Item label={INPUT_1_LABEL} colon={false} help={codeValidation === HAS_ERROR ? mfaCodeWarning : null}>
              <Input placeholder="123456" value={code1} onChange={handleCode1Change} />
            </Form.Item>
          </div>
        </div>
        {restSessionEnrollPost?.error && <Alert message={restSessionEnrollPost.error || ERROR_TEXT} type="error" />}
      </div>
    );
  };

  const renderMFASetupModal = useMemo(() => {
    return (
      <AntModal
        visible={showSetupModal}
        title={SETUP_MFA_MODAL_TEXT}
        footer={renderSetupModalFooter}
        width={700}
        closable={false}
        wrapClassName="mfa-setup-modal">
        {showResult && renderResult()}
        {!showResult && renderOriginalContent()}
      </AntModal>
    );
  }, [code1, showResult, showSetupModal, restSessionEnrollGet, restSessionEnrollPost]);

  return <div className="setup-modal-container">{renderMFASetupModal}</div>;
};

export default MFASetupModal;
