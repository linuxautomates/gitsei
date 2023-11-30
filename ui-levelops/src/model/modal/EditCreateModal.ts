export interface EditCreateModalPayload {
  name: string;
  description?: string;
  default?: boolean;
  data?: { [key: string]: any };
}

export interface EditCreateModalFormInfo {
  nameLabel?: string;
  namePlaceholder: string;
  descriptionLabel?: string;
  descriptionPlaceholder?: string;
  defaultLabel?: string;
  disableDefault?: boolean;
}
