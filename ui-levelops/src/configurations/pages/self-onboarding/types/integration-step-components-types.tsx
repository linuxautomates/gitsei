import React, { ReactNode } from "react";
import { SelfOnboardingFormFields } from "../constants";

export type IntegrationNavButtonType = {
  title: string;
  hidden?: (args: any) => boolean;
  type: "primary" | "default";
  isDisabled?: (args: any) => boolean;
  onClick: (args: any) => () => void;
};

export type IntegrationStepsConfigType = {
  title: ((args: string) => string) | string;
  description?: ReactNode;
  component: React.FC<any>;
  titleClassName?: string;
  nav_buttons: Array<IntegrationNavButtonType>;
  required?: Array<SelfOnboardingFormFields>;
};

export type DefinitionConfigType = {
  key: string;
  value: string;
  checked: boolean;
};

export type AuthorizationConfigType = {
  oauth?: string;
  pac?: string;
  client_id: string;
  scope: Array<string>;
  default_url?: string;
};

export type IntegrationCreatePayloadParamsType = {
  selfOnboardingForm: any;
  application: string;
  code: string | string[] | null;
  state: string | string[] | null;
  extraPayload: any;
  isUpdate: boolean;
};

export type SCMReposDataType = {
  name: string;
  url: string;
  updated_at: number;
  description: string;
};

export type ScmReposConfigType = {
  records: Array<SCMReposDataType>;
  _metadata: { total_count: number };
};
