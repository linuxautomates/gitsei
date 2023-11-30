import { InputNumber } from "antd";
import { RestTrellisProfileSections, RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { get } from "lodash";
import React, { useMemo } from "react";
import { AntSwitch, AntTable, AntText } from "shared-resources/components";
import { SECTIONS, TRELLIS_SECTION_MAPPING } from "../constant";

interface AssociationsProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (section_name: string, value: any, type: string) => void;
  disabled?: boolean;
}

const Factors: React.FC<AssociationsProps> = ({ profile, handleChanges, disabled }) => {
  const dataSource = useMemo(() => {
    return SECTIONS.map((section: string) => {
      const storedSection = profile?.sections?.find((sec: RestTrellisProfileSections) => sec.name === section);
      if (storedSection) {
        return {
          name: get(TRELLIS_SECTION_MAPPING, [storedSection.name ?? ""], storedSection.name),
          enabled: storedSection.enabled,
          weight: storedSection.weight || 0,
          key: storedSection.name
        };
      }
      return {
        name: section,
        enabled: false,
        weight: 0,
        key: section
      };
    });
  }, [profile]);
  const _columns = [
    {
      title: "Weight (1-10)",
      dataIndex: "weight",
      key: 1,
      render: (item: any, record: any, index: any) => {
        return (
          <InputNumber
            min={0}
            max={10}
            value={item}
            style={{ border: "1px solid #aeadad" }}
            onChange={(value: any) => handleChanges(record.name, value, "weight")}
            disabled={disabled}>
            {item}
          </InputNumber>
        );
      }
    },
    {
      title: "Area",
      dataIndex: "name",
      key: 2,
      render: (item: any, record: any, index: any) => {
        return <AntText style={{ color: "#2967DD" }}>{item}</AntText>;
      },
      ellipsis: true
    },
    {
      key: 3,
      align: "center",
      title: "Enable",
      dataIndex: "enabled",
      width: 100,
      render: (item: any, record: any, index: any) => {
        return (
          <AntSwitch
            checked={item}
            disabled={disabled}
            onChange={(value: any) => handleChanges(record.name, value, "enable")}
          />
        );
      }
    }
  ];
  return (
    <div className="dev-score-profile-container-section">
      <div className="dev-score-profile-container-section-container">
        <div className="dev-score-profile-container-section-container-header">
          <AntText className="section-header">{"FACTORS & WEIGHTS"}</AntText>
          <AntText className="section-sub-header">
            Customize the weights for the areas that make up the total Trellis score.
          </AntText>
        </div>

        <div className="dev-score-profile-container-section-container-body">
          <AntTable className="category-list" columns={_columns} dataSource={dataSource} pagination={false} />
        </div>
      </div>
    </div>
  );
};

export default Factors;
