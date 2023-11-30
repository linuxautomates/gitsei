import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Divider, Icon, Modal, Switch } from "antd";
import "./ConfigureFactorModal.scss";
import { AntInput, SvgIcon } from "shared-resources/components";

import { FACTOR_NAME_TO_ICON_MAPPING, SECTIONS } from "../constant";
import { cloneDeep } from "lodash";

export interface ConfigureFactorModalProps {
  visible: boolean;
  onClose: () => void;
  currentSubProfile: any;
  subProfileFactorsUpdate: (param: any) => void;
}

export const ConfigureFactorModal: React.FC<ConfigureFactorModalProps> = ({
  visible = false,
  onClose,
  currentSubProfile,
  subProfileFactorsUpdate
}) => {
  const [subProfile, setSubProfile] = useState<any>(undefined);

  useEffect(() => {
    setSubProfile(cloneDeep(currentSubProfile));
  }, []);

  const updateSubProfile = useCallback(
    (key: string, value: any, index: number) => {
      const newSubProfile: any = cloneDeep(subProfile);
      const factor = { ...newSubProfile?.sections?.[index] };
      if (key === "weight") {
        factor[key] = value;
      }
      if (key === "enabled") {
        factor[key] = value;
        factor.features.forEach((feature: any) => {
          feature[key] = value;
        });
      }
      if (newSubProfile?.sections?.[index]) {
        newSubProfile.sections[index] = factor;
        setSubProfile(newSubProfile);
      }
    },
    [subProfile]
  );

  const factorsRows = useMemo(() => {
    return subProfile?.sections?.map((section: any, index: number) => {
      return (
        <div key={section?.name} className="flex modal-row">
          <div className="label-info">
            <Switch checked={section?.enabled} onChange={() => updateSubProfile("enabled", !section?.enabled, index)} />
            <SvgIcon icon={FACTOR_NAME_TO_ICON_MAPPING[section?.name]} />
            <span className="label">{section?.name}</span>
          </div>
          <div className="factor-weight">
            <div>
              <span className="label">Weightage : </span>
              <Icon type="info-circle" />
            </div>
            <AntInput
              disabled={!section?.enabled}
              min={0}
              max={10}
              key={section?.name}
              type="number"
              onChange={(e: any) => updateSubProfile("weight", e, index)}
              value={section?.weight}
            />
          </div>
        </div>
      );
    });
  }, [subProfile]);

  return (
    <Modal
      title={"Configure Factors"}
      visible={visible}
      className="configure-factor"
      cancelText="Cancel"
      okText="Save"
      width={"50rem"}
      maskClosable={false}
      onCancel={onClose}
      footer={[
        <Button type="primary" onClick={() => subProfileFactorsUpdate(subProfile)}>
          Save
        </Button>,
        <Button key="submit" onClick={onClose}>
          Cancel
        </Button>
      ]}>
      {factorsRows}
      <Divider />
    </Modal>
  );
};
