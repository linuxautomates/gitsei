import { Button } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";

interface TrellisDisabledPageProps {
  setTrellisProfileIsEnabled: () => void;
}
export const TrellisDisabledPage: React.FC<TrellisDisabledPageProps> = ({ setTrellisProfileIsEnabled }) => {
  return (
    <div className="trellis-disabled-wrapper">
      <SvgIconComponent icon={"trellisDisabled"} />
      <label>Enable the Trellis Scoring for the collection</label>
      <div className="note">
        Looks like the Trellis scoring is not enabled for this collection. <br />
        Enabling Trellis scoring allows to score the contributors in this collection
      </div>
      <div className="button-wrapper">
        <Button onClick={setTrellisProfileIsEnabled} type={"primary"}>
          Enable Trellis for the collection
        </Button>
        <Button type={"link"}>Learn more about Trellis Scoring</Button>
      </div>
    </div>
  );
};
