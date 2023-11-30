export type WorkspaceIntegrationType = {
  id: string;
  name: string;
  application: string;
  append_metadata: boolean;
};

export interface WorkspaceModel {
  id: string;
  name: string;
  description: string;
  key: string;
  owner_id: string;
  integration_ids: Array<number>;
  integrations: Array<WorkspaceIntegrationType>;
  updated_at: number;
  created_at: number;
  demo?: boolean;
  bootstrapped: boolean;
  immutable: boolean;
  orgIdentifier?: string;
}

export type WorkspaceMethodType = "list" | "get" | "update" | "delete" | "create" | "bulkDelete";

type WorkspaceListDataType = {
  page: number;
  page_size: number;
  total_count: number;
  count: number;
  results: Array<WorkspaceModel>;
};

export type WorkspaceResponseType = WorkspaceModel | string | WorkspaceListDataType;
export interface WorkspaceDataType {
  loading: boolean;
  error: boolean;
  data: WorkspaceResponseType;
}

export type WorkspaceInitialStateType = Record<string, Record<string, WorkspaceDataType> | WorkspaceModel>;

export type WorkspaceActionType = {
  type: string;
  id: string;
  method?: WorkspaceMethodType;
  payload: Record<string, WorkspaceDataType> | WorkspaceModel | string;
};

export type WorkspaceHandlerType = (
  state: WorkspaceInitialStateType,
  action: WorkspaceActionType
) => WorkspaceInitialStateType;
