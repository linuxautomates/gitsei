import { isArray } from "lodash";
import React from "react";
import { AntCol, AntPopover, AntRow, NameAvatar, NameAvatarList } from "../../../components";
import { newUXColorMapping } from "../../chart-themes";

interface TeamAllocationColumnProps {
  teams: any[];
}

const TeamAllocationColumn: React.FC<TeamAllocationColumnProps> = (props: TeamAllocationColumnProps) => {
  const { teams } = props;
  const extraTeamsSlice = (teams || []).length > 3 ? (teams || []).slice(3, (teams || []).length + 1) : [];
  return (
    <div style={{ textAlign: "center" }}>
      {isArray(teams) && (teams || []).length > 0 ? (
        <div className="teams-container">
          <div
            style={{ display: "flex", alignItems: "center", marginRight: (teams || []).length > 3 ? "4rem" : "6rem" }}>
            <p style={{ color: newUXColorMapping["blue-secondary"], marginRight: "0.5rem", width: "max-content" }}>
              {teams.length}
            </p>
            <NameAvatarList
              names={teams.map((item: any) => item.name).slice(0, 3)}
              namesShowFactor={3}
              classRequired={false}
              className={"team-div"}
            />
          </div>
          {teams.length > 3 && (
            <AntPopover
              trigger="hover"
              title={null}
              placement={"right"}
              content={
                <div
                  className="team-popover-content"
                  style={{ overflowY: extraTeamsSlice.length > 2 ? "scroll" : "hidden" }}>
                  {(teams || []).slice(3, teams.length + 1).map((person: any) => (
                    <div style={{ width: "10rem", padding: "0.6rem" }}>
                      <AntRow align={"middle"} type={"flex"} justify={"space-evenly"}>
                        <AntCol span={6}>
                          <NameAvatar name={person?.name} showTooltip={false} />
                        </AntCol>
                        <AntCol span={14}>
                          <span style={{ color: "#2967DD" }}>{person?.name}</span>
                        </AntCol>
                      </AntRow>
                    </div>
                  ))}
                </div>
              }>
              <p className="teams-remaining">(+{teams.length - 3})</p>
            </AntPopover>
          )}
        </div>
      ) : (
        <span style={{ color: newUXColorMapping.grey }}>0</span>
      )}
    </div>
  );
};

export default React.memo(TeamAllocationColumn);
