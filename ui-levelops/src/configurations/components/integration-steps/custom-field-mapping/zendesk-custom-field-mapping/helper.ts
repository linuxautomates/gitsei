export const DelimiterOptions = [
  { label: "None", value: "none" },
  { label: "Comma (,)", value: "," },
  { label: "Semicolon (;)", value: ";" },
  { label: "Colon (:)", value: ":" },
  { label: "Single quote (')", value: "'" },
  { label: "Pipe (|)", value: "\\|" }
];

export enum ZendeskCustomMappingActionType {
  DELETE = "delete"
}

export const headerStyle = {
  fontSize: "1rem",
  fontWeight: "bold",
  textTransform: "uppercase"
};

export const dropdownStyle = (showDropdown: boolean | undefined) => (showDropdown ? {} : { display: "none" });

export const extraTitleStyle = {
  justifyContent: "flex-start"
};

export const TITLE_DESCRIPTION =
  "Zendesk custom fields are not ingested by default.\nSelect custom fields required for data analysis using SEI Zendesk widgets.";
