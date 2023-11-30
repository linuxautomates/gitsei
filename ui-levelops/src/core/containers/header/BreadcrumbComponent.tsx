import React from "react";
import { Breadcrumb } from "antd";
import { Link } from "react-router-dom";
import { getIsStandaloneApp } from "helper/helper";

export type BreadCrumbType = {
  label: string;
  path: string;
  customOnClick?: () => void | null;
};

const BreadCrumbComponent: React.FC<{ breadcrumbs: BreadCrumbType[] }> = ({ breadcrumbs }) => {
  const isStandaloneApp = getIsStandaloneApp();
  const filteredBreadcrumbs = breadcrumbs.filter(item => isStandaloneApp || !item.path?.endsWith("configuration"));
  return (
    <Breadcrumb className="mb-5">
      {filteredBreadcrumbs.map((item, index) => (
        <Breadcrumb.Item key={index}>
          {item?.customOnClick ? (
            <Link
              color="inherit"
              to={item.path}
              key={item.path}
              onClick={e => {
                e.preventDefault();
                (item as any)?.customOnClick(e);
              }}
              style={{
                color: index !== filteredBreadcrumbs.length - 1 ? "#8c8c8c" : "var(--harness-blue)",
                fontSize: "14px",
                lineHeight: "22px"
              }}>
              {item.label}
            </Link>
          ) : (
            <Link
              color="inherit"
              to={item.path}
              key={item.path}
              style={{
                color: index !== filteredBreadcrumbs.length - 1 ? "#8c8c8c" : "var(--harness-blue)",
                fontSize: "14px",
                lineHeight: "22px"
              }}>
              {item.label}
            </Link>
          )}
        </Breadcrumb.Item>
      ))}
    </Breadcrumb>
  );
};

export default React.memo(BreadCrumbComponent);
