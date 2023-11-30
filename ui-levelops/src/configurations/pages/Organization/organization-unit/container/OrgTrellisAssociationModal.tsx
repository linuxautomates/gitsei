import React, { useEffect, useState } from "react";
import { Modal } from "antd";
import TrellisScoreProfileLandingPage from "configurations/pages/TrellisProfile/LandingPage/TrellisScoreProfileLandingPage";
import { AntRadioGroup } from "shared-resources/components";
import { useDispatch } from "react-redux";
import { trellisProfileOuAssociationAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";

interface OrgTrellisAssociationModalProps {
  showModal: boolean;
  onCancel: () => void;
  onAssociationSuccess: () => void;
  org?: orgUnitJSONType;
}

const OrgTrellisAssociationModal: React.FC<OrgTrellisAssociationModalProps> = ({
  showModal,
  onCancel,
  onAssociationSuccess,
  org
}) => {
  const [selectedProfile, setSelectedProfile] = useState();
  const dispatch = useDispatch();

  useEffect(() => {
    setSelectedProfile(undefined);
  }, [org]);

  const onAssociateClick = () => {
    selectedProfile && dispatch(trellisProfileOuAssociationAction(selectedProfile, org?.id || "", org?.name || ""));
    onAssociationSuccess();
  };
  return (
    <Modal
      visible={showModal}
      onOk={onAssociateClick}
      onCancel={onCancel}
      okText="Associate Profile"
      cancelText="Cancel"
      title="Select trellis profile for association"
      okButtonProps={{
        disabled: !selectedProfile
      }}>
      <AntRadioGroup value={selectedProfile} onChange={(e: any) => setSelectedProfile(e.target.value)}>
        <TrellisScoreProfileLandingPage isModalView />
      </AntRadioGroup>
    </Modal>
  );
};

export default OrgTrellisAssociationModal;
