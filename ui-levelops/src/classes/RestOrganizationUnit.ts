import {
  defaultUserSectionConfig,
  managersConfigType,
  OrgUnitSectionPayloadType,
  OUDashboardType,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import { sanitizeObject } from "utils/commonUtils";

export class RestOrganizationUnit {
  _id?: string;
  _ou_id?: string;
  _name?: string;
  _description?: string;
  _draft?: boolean;
  _admins?: managersConfigType[];
  _managers?: managersConfigType[];
  _tags?: string[];
  _sections?: OrgUnitSectionPayloadType[];
  _version: string;
  _default_section: defaultUserSectionConfig;
  _ou_category_id?: string;
  _dashboards?: Array<OUDashboardType>;
  _parent_ref_id?: number;
  _valid_name?: boolean;
  _is_parent?: boolean;
  _has_childs?: boolean;
  _default_dashboard_id?: string;
  _access_response?: {
    create: boolean;
    edit: boolean;
    delete: boolean;
    view: boolean;
  };

  constructor(restdata: any) {
    this._id = "";
    this._ou_id = "";
    this._name = "";
    this._description = "";
    this._managers = [];
    this._admins = [];
    this._tags = [];
    this._sections = [];
    this._version = "1";
    this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    this._ou_category_id = "";
    this._dashboards = [];
    this._valid_name = true;
    this._is_parent = false;
    this._has_childs = false;
    this._default_dashboard_id = "";
    this._access_response = {
      create: true,
      edit: true,
      delete: true,
      view: true
    };

    if (!!restdata) {
      this._id = (restdata || ({} as any)).id;
      this._ou_id = (restdata || ({} as any)).ou_id;
      this._name = (restdata || ({} as any)).name;
      this._description = (restdata || ({} as any)).description;
      this._managers = (restdata || ({} as any)).managers;
      this._admins = (restdata || ({} as any)).admins;
      this._tags = (restdata || ({} as any)).tags;
      this._sections = (restdata || ({} as any)).sections;
      this._version = (restdata || ({} as any)).version;
      this._default_section = (restdata || ({} as any)).default_section;
      this._ou_category_id = (restdata || ({} as any)).ou_category_id;
      this._dashboards = (restdata || ({} as any)).dashboards;
      this._parent_ref_id = (restdata || ({} as any)).parent_ref_id;
      this._valid_name = (restdata || ({} as any)).valid_name;
      this._is_parent = (restdata || ({} as any)).is_parent;
      this._has_childs = (restdata || ({} as any)).has_childs;
      this._default_dashboard_id = (restdata || ({} as any)).default_dashboard_id;
      this._access_response = (restdata || ({} as any)).access_response;
    }
  }

  get access_response() {
    return this._access_response;
  }

  get id() {
    return this._id;
  }

  get isParent() {
    return this._is_parent;
  }

  get hasChilds() {
    return this._has_childs;
  }

  set id(id: string | undefined) {
    this._id = id;
  }

  get validName() {
    return this._valid_name ?? true;
  }

  set validName(valid: boolean) {
    this._valid_name = valid;
  }
  get ouId() {
    return this._ou_id;
  }

  set ouId(id: string | undefined) {
    this._ou_id = id;
  }

  get ouGroupId() {
    return this._ou_category_id;
  }

  set ouGroupId(groupId: string | undefined) {
    this._parent_ref_id = undefined;
    this._ou_category_id = groupId;
  }

  get version() {
    return this._version;
  }

  set version(version: string) {
    this._version = version;
  }

  get name() {
    return this._name;
  }

  set name(gname: string | undefined) {
    this._name = gname;
  }

  get defaultDashboardId() {
    return this._default_dashboard_id;
  }

  set defaultDashboardId(id: string | undefined) {
    this._default_dashboard_id = id;
  }

  get description() {
    return this._description;
  }

  set description(gdesc: string | undefined) {
    this._description = gdesc;
  }

  get managers() {
    return this._managers;
  }

  get admins() {
    return this._admins;
  }

  set managers(manager: managersConfigType[] | undefined) {
    this._managers = manager;
  }

  get dashboards() {
    return this._dashboards ?? [];
  }

  set dashboards(ouDashboards: Array<OUDashboardType>) {
    this._dashboards = ouDashboards;
  }

  get tags() {
    return this._tags;
  }

  set tags(tags: string[] | undefined) {
    this._tags = tags;
  }

  get parentId() {
    return this._parent_ref_id;
  }

  set parentId(id: number | undefined) {
    this._parent_ref_id = id;
  }

  get sections() {
    return this._sections;
  }

  set sections(sectionList: OrgUnitSectionPayloadType[] | undefined) {
    this._sections = sectionList;
  }

  get default_section() {
    return this._default_section || { dynamic_user_definition: [], users: [] };
  }

  set default_section(defaultSection: defaultUserSectionConfig) {
    this._default_section = defaultSection;
  }

  get dynamic_user_definition() {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    return this._default_section?.dynamic_user_definition;
  }

  set dynamic_user_definition(selectedUserAttributes: sectionSelectedFilterType[] | undefined) {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    this._default_section.dynamic_user_definition = selectedUserAttributes;
  }

  get users() {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    return this._default_section?.users;
  }

  set users(nusers: string[] | undefined) {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    this._default_section.users = nusers;
  }

  get csv_users() {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    return this._default_section?.csv_users;
  }

  set csv_users(data: any) {
    if (!this._default_section) {
      this._default_section = { dynamic_user_definition: [], users: [], csv_users: {} };
    }
    this._default_section.csv_users = data;
  }

  get json() {
    return sanitizeObject({
      id: this._id,
      ou_id: this._ou_id,
      name: this._name,
      description: this._description,
      managers: this._managers,
      tags: this._tags,
      version: this._version,
      sections: this._sections,
      default_section: this._default_section,
      ou_category_id: this._ou_category_id,
      dashboards: this.dashboards,
      parent_ref_id: this._parent_ref_id?.toString(),
      valid_name: this._valid_name,
      is_parent: this._is_parent,
      has_childs: this._has_childs,
      admins: this._admins,
      default_dashboard_id: this._default_dashboard_id,
      access_response: this._access_response
    });
  }
}
