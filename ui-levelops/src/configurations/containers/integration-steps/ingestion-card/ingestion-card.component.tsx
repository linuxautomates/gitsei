import React from "react";
import { AntCard } from "../../../../shared-resources/components";
import Loader from "../../../../components/Loader/Loader";

import "./ingestion-card.style.scss";

interface IngestionCardProps {
  title: string;
  value: string | number;
  loading?: boolean;
}

const IngestionCardComponent: React.FC<IngestionCardProps> = ({ title, value, loading }) => {
  return (
    <AntCard className="stat-card" bordered>
      <div className="stat-card--title">{title}</div>
      {loading && <Loader />}
      {!loading && <div className="stat-card--content flex align-center justify-left">{value}</div>}
    </AntCard>
  );
};

export default React.memo(IngestionCardComponent);
