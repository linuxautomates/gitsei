import React from "react";

import { AntCard } from "shared-resources/components";
import GeneralViolationsCard from "./general-violations-card.component";
import { generalViolationsData } from "../../../mock-data";

const GeneralViolations = props => {
  return (
    <AntCard title="General violations">
      {generalViolationsData.map((d, i) => {
        return (
          <GeneralViolationsCard
            key={i}
            className="mb-8"
            icontype={d.iconType}
            title={d.title}
            description={d.description}
          />
        );
      })}
    </AntCard>
  );
};

export default GeneralViolations;
