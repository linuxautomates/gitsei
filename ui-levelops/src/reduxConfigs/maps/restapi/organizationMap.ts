import { OrganizationUnitList, OrganizationUnitRestGet } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { OrganizationUnitListFilterType } from "reduxConfigs/types/map-types/organizationMap.types";

export const mapOrgUnitToProps = (dispatch: any) => {
  return {
    orgUnitList: (filters: OrganizationUnitListFilterType, id: string) => dispatch(OrganizationUnitList(filters, id)),
    orgUnitGet: (id: string) => dispatch(OrganizationUnitRestGet(id))
  };
};
