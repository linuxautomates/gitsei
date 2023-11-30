import { Integration } from "model/entities/Integration";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";
import { BaseReducerState } from "../base/baseReducerState";
import { EntityState } from "../base/EntityState";

export interface IntegrationActionType extends BaseActionType {
  loading?: boolean;
  error?: boolean;
  expires_in?: number;
  integration_id?: string;
  payload?: EntityState<Integration>;
}

export interface IntegrationState extends BaseReducerState {
  expires_in: number;
  cached_at: number;
  data: EntityState<Integration>;
  hasAllIntegrations?: boolean;
}

export type IntegrationHandlerType = (state: IntegrationState, action: IntegrationActionType) => IntegrationState;
