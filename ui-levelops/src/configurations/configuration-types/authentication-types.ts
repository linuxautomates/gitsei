export type ConfigStateType = {
  id: string;
  name: string;
  value: string | number;
};

export enum ConfigStateNameType {
  MFA_ENFORCED = "MFA_ENFORCED"
}
