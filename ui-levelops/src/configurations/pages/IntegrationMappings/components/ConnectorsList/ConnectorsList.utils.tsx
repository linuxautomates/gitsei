import type { CellProps, Renderer, HeaderProps } from "react-table";
import { Layout, FontVariation, Text, Container, Checkbox } from "@harness/uicore";
import { IntegrationIcon } from "shared-resources/components";
import React from "react";
import { humanizeDuration } from "utils/timeUtils";
import "./ConnectorsList.scss";
import Status from "./Status";

//@ts-ignore
export const RenderIntName: Renderer<CellProps<any>> = ({ row }) => {
  const { application, name, id, status } = row.original;
  return (
    <Layout.Horizontal flex={{ justifyContent: "left", alignItems: "center" }} margin={{ left: "medium" }}>
      {
        // @ts-ignore
        <IntegrationIcon type={application} />
      }
      <Container className="integrationName">
        <Text font={{ variation: FontVariation.BODY2_SEMI }}>{name}</Text>
        <Text>{`ID: ${id}`}</Text>
      </Container>
    </Layout.Horizontal>
  );
};

//@ts-ignore
export const RenderStatus: Renderer<CellProps<any>> = ({ row }) => {
  const { id } = row.original;
  return <Status id={id} />;
};

const checkboxClick =
  (id: string, selectedIntegrations: number[], onSelectionChange?: (selectedIntegrations: number[]) => void) =>
  (e: React.MouseEvent<HTMLInputElement, MouseEvent>) => {
    e?.stopPropagation();
    let updatedSelection = [];
    if (e.currentTarget.checked) {
      updatedSelection = [...selectedIntegrations, Number.parseInt(id)];
    } else {
      updatedSelection = selectedIntegrations.filter(intId => intId.toString() !== id);
    }
    onSelectionChange && onSelectionChange(updatedSelection);
  };

export const CheckboxCell = (
  selectedIntegrations: number[],
  onSelectionChange?: (selectedIntegrations: number[]) => void
): Renderer<CellProps<any>> => {
  const cell: Renderer<CellProps<any>> = ({ row }) => {
    const { id } = row.original;
    const checked = selectedIntegrations.map(intId => intId.toString()).includes(id);
    return (
      <Checkbox
        margin={{ top: "small", left: "small" }}
        checked={checked}
        disabled={!onSelectionChange}
        onClick={checkboxClick(id, selectedIntegrations, onSelectionChange)}
      />
    );
  };
  return cell;
};

export const updatedAtCell: Renderer<CellProps<any>> = ({ row }) => {
  const { updated_at } = row.original;
  return <Text font={{ variation: FontVariation.BODY2 }}>{humanizeDuration(updated_at)}</Text>;
};

export const statusCell: Renderer<CellProps<any>> = ({ row }) => {
  const { status, statusUpdated } = row.original;
  if (!!statusUpdated) {
    return <h1>{status}</h1>;
  } else {
    return <h1>Loading...</h1>;
  }
};

export const RenderHeader = (title: string): Renderer<HeaderProps<any>> => {
  const header: Renderer<HeaderProps<any>> = () => {
    return (
      <Text font={{ variation: FontVariation.TABLE_HEADERS }} padding={{ left: "medium" }}>
        {title}
      </Text>
    );
  };
  return header;
};

export function getFilter(workspaceIntegrations?: number[], searchItem?: string) {
  let filter = {};
  let integrationFilter: { integration_ids?: string[] } = {};
  if (workspaceIntegrations?.length) {
    integrationFilter = {
      integration_ids: workspaceIntegrations.map(id => id.toString())
    };
  }
  if (searchItem || integrationFilter?.integration_ids?.length) {
    filter = {
      partial: {
        ...(searchItem ? { name: searchItem } : {})
      },
      ...(integrationFilter.integration_ids?.length ? integrationFilter : {})
    };
  }

  return filter;
}
