export interface SelectIntegrationTypeConfig {
  [x: string]: {
    title: string;
    subHeading: string;
    integrationTypes: {
      id: string;
      icon: string;
      typeName: string;
      typeNameInfo: string;
      iconProps?: any;
    }[];
  };
}
