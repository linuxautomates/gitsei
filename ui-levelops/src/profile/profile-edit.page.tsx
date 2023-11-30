import { Divider, Form, Input, Popconfirm } from "antd";
import { RestUsers } from "classes/RestUsers";
import { EMPTY_FIELD_WARNING } from "constants/formWarnings";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { userProfileGet, userProfileUpdate } from "reduxConfigs/actions/restapi";
import { resetPassword } from "reduxConfigs/actions/sessionActions";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { userProfileData, userProfileUpdateState } from "reduxConfigs/selectors/usersSelector";
import { MeResponse } from "reduxConfigs/types/response/me.response";
import LocalStoreService from "services/localStoreService";
import { AntButton, AntCard, AntModal, AntRow, AntText } from "shared-resources/components";
import { isSanitizedValue } from "utils/commonUtils";
import { formWarming } from "views/Pages/common/commons.component";
import MFASetupModal from "./mfa-setup-modal/MFASetupModal";
import "./profile-edit.style.scss";

type ProfileFormType = {
  first_name: string;
  last_name: string;
};

const HAS_ERROR = "hasError";

const ProfileEdit: React.FC = () => {
  const dispatch = useDispatch();
  const ls = new LocalStoreService();

  const company = ls.getUserCompany();

  const profile: MeResponse = useSelector(userProfileData);
  const profileUpdateState = useParamSelector(userProfileUpdateState);

  const [forgotPassword, setForgotPassword] = useState<boolean>(false);
  const [profileForm, setProfileForm] = useState<ProfileFormType>();
  const [formValidation, setFormValidation] = useState<ProfileFormType>();
  const [loading, setLoading] = useState<boolean>(false);
  const [showMFASetupModal, setShowMFASetupModal] = useState<boolean>(false);

  const getMe = () => dispatch(userProfileGet());

  useEffect(() => {
    setLoading(true);
    getMe();
  }, []);

  useEffect(() => {
    if (!profileForm && isSanitizedValue(profile?.id)) {
      setProfileForm({ first_name: profile.first_name, last_name: profile.last_name });
      setLoading(false);
    }
  }, [profile]);

  useEffect(() => {
    if (profileUpdateState && loading) {
      const { loading, error } = profileUpdateState;
      if (loading === false) {
        setLoading(false);
        getMe();
      }
    }
  }, [profileUpdateState]);

  const renderForgotPassword = useMemo(
    () => (
      <AntModal
        closable
        centered
        title="Password Reset Instructions"
        visible={forgotPassword}
        footer={null}
        onCancel={() => setForgotPassword(false)}
        onOk={() => setForgotPassword(false)}>
        Please check your email and click on the reset password link
      </AntModal>
    ),
    [forgotPassword]
  );

  const handleChange = useCallback(
    (event: any) => {
      event?.persist?.();
      const field = event?.target?.id;
      if (isSanitizedValue(field)) {
        setProfileForm((prev: any) => ({ ...prev, [field]: event?.target?.value || "" }));
        switch (field) {
          case "first_name":
          case "last_name":
            setFormValidation((prev: any) => ({
              ...prev,
              [field]: (event?.target?.value || "").length > 0 ? "" : HAS_ERROR
            }));
            break;
          default:
            break;
        }
      }
    },
    [profileForm, formValidation]
  );

  const handleUpdate = useCallback(() => {
    const restUser = new RestUsers({
      ...profile,
      first_name: profileForm?.first_name,
      last_name: profileForm?.last_name
    });
    dispatch(userProfileUpdate(restUser));
    setLoading(true);
  }, [profileForm, profile]);

  const handleDisableMFA = useCallback(() => {
    const restUser = new RestUsers({
      ...profile,
      mfa_enabled: false
    });
    dispatch(userProfileUpdate(restUser));
    setLoading(true);
  }, [profile]);

  const handleForgotPassword = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      if (isSanitizedValue(profile?.email) && isSanitizedValue(company)) {
        dispatch(resetPassword(profile.email, company));
        setForgotPassword(true);
      }
    },
    [company, profile]
  );

  const closeMFAEnrollModal = () => setShowMFASetupModal(false);

  const emptyWarning = useMemo(() => formWarming(EMPTY_FIELD_WARNING), []);

  return (
    <>
      {showMFASetupModal && (
        <MFASetupModal
          onMFAEnrollSuccess={getMe}
          showSetupModal={showMFASetupModal}
          closeSetupModal={closeMFAEnrollModal}
        />
      )}
      <div className={`flex direction-column align-center`}>
        <div className={`profile-edit-page__content`}>
          <AntCard
            title={"User Profile"}
            extra={
              <AntButton
                onClick={handleUpdate}
                type={"primary"}
                disabled={profileForm?.first_name === "" || profileForm?.last_name === "" || loading}>
                Update
              </AntButton>
            }>
            {forgotPassword && renderForgotPassword}
            <AntRow className={"mb-20"}>
              <AntText secondary>
                Company <b>{company?.toUpperCase()}</b>
              </AntText>
            </AntRow>
            <Form layout={"vertical"}>
              <Form.Item label="Email Address" required>
                <Input value={profile?.email} disabled id="email" placeholder="Email Address" />
              </Form.Item>
              <Form.Item
                required
                label="First Name"
                help={formValidation?.first_name === HAS_ERROR ? emptyWarning : null}>
                <Input
                  disabled={loading}
                  onChange={handleChange}
                  value={profileForm?.first_name}
                  id="first_name"
                  placeholder="First Name"
                />
              </Form.Item>
              <Form.Item
                required
                label="Last Name"
                help={formValidation?.last_name === HAS_ERROR ? emptyWarning : null}>
                <Input
                  disabled={loading}
                  onChange={handleChange}
                  value={profileForm?.last_name}
                  id="last_name"
                  placeholder="Last Name"
                />
              </Form.Item>
              <Divider />
              <div className={"flex direction-column py-20"}>
                <span style={{ fontSize: 16, fontWeight: 500 }}>Password Settings</span>
                <span className={"mt-10"}>
                  <span className="link" onClick={handleForgotPassword}>
                    Click Here{" "}
                  </span>
                  to receive password reset instructions.
                </span>
              </div>
              <Divider />
              <div className={"flex direction-column py-20"}>
                <span style={{ fontSize: 16, fontWeight: 500 }}>MFA Settings</span>
                <div className={"mt-10"}>
                  {profile?.mfa_enabled ? (
                    <Popconfirm
                      onConfirm={handleDisableMFA}
                      title="Are you sure you want to disable MFA?"
                      okText="Confirm">
                      <AntButton
                        type={"danger"}
                        ghost
                        disabled={loading || !profile?.mfa_enabled || profile.mfa_enforced}>
                        Disable MFA
                      </AntButton>
                    </Popconfirm>
                  ) : (
                    <AntButton
                      onClick={() => setShowMFASetupModal(true)}
                      type={"ghost"}
                      disabled={loading || profile?.mfa_enabled}>
                      Enable MFA
                    </AntButton>
                  )}
                </div>
              </div>
            </Form>
          </AntCard>
        </div>
      </div>
    </>
  );
};

export default ProfileEdit;
