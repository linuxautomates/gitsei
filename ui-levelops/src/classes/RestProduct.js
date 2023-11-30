import { IntegrationTypes } from "constants/IntegrationTypes";
import { validateEmail } from "../utils/stringUtils";

export class RestMapping {
  constructor(restData = null) {
    this._id = undefined;
    this._stage_id = undefined;
    this._integration_id = undefined;
    this._integration_type = undefined;
    this._product_id = undefined;
    this._mappings = {};
    if (restData) {
      this._id = restData?.id;
      this._stage_id = restData?.stage_id;
      this._integration_id = restData?.integration_id;
      this._integration_type = restData?.integration_type;
      this._product_id = restData?.product_id;
    }
    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
  }

  get id() {
    return this._id;
  }
  set id(id) {
    this._id = id;
  }

  get stage_id() {
    return this._stage_id;
  }
  set stage_id(id) {
    this._stage_id = id;
  }

  get product_id() {
    return this._product_id;
  }
  set product_id(id) {
    this._product_id = id;
  }

  get integration_id() {
    return this._integration_id;
  }
  set integration_id(id) {
    this._integration_id = id;
  }

  get integration_type() {
    return this._integration_type;
  }
  set integration_type(type) {
    this._integration_type = type;
  }

  get mappings() {
    return this._mappings;
  }
  set mappings(map) {
    this._mappings = map;
  }

  static validate(data) {
    let valid = true;
    //valid = valid && data.hasOwnProperty("stage_id");
    valid = valid && data.hasOwnProperty("integration_id");
    valid = valid && data.hasOwnProperty("integration_type");
    return valid;
  }

  validate() {
    let valid = true;
    valid = valid && this._stage_id !== undefined;
    console.log(`valid ${valid} ${this._stage_id}`);
    valid = valid && this._integration_id !== undefined;
    console.log(`valid ${valid} ${this._integration_id}`);
    valid = valid && this._integration_type !== undefined;
    console.log(`valid ${valid} ${this._integration_type}`);
    return valid;
  }

  json() {
    return {
      id: this._id,
      //stage_id: this._stage_id,
      integration_id: this._integration_id,
      integration_type: this._integration_type,
      product_id: this._product_id,
      mappings: this._mappings
    };
  }
}

export class RestJiraMapping extends RestMapping {
  static MAPPINGS = Object.freeze(["product", "release", "feature", "sprint"]);

  constructor(restData = null) {
    super(restData);
    this._integration_type = IntegrationTypes.JIRA;
    this._mappings = {
      product: { field_name: undefined, field_id: undefined, regex: "" },
      release: { field_name: undefined, field_id: undefined, regex: "" },
      feature: { field_name: undefined, field_id: undefined, regex: "" },
      sprint: { field_name: undefined, field_id: undefined, regex: "" }
    };
    if (restData) {
      this._mappings = restData?.mappings;
    }
    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
    this.resetDefinitions = this.resetDefinitions.bind(this);
  }

  get product() {
    return this._mappings.product;
  }
  set product(product) {
    this._mappings.product = product;
  }

  get release() {
    return this._mappings.release;
  }
  set release(release) {
    this._mappings.release = release;
  }

  get feature() {
    return this._mappings.feature;
  }
  set feature(feature) {
    this._mappings.feature = feature;
  }

  get sprint() {
    return this._mappings.sprint;
  }
  set sprint(sprint) {
    this._mappings.sprint = sprint;
  }

  get id() {
    return super.id;
  }
  set id(id) {
    this._id = id;
  }

  get stage_id() {
    return super.stage_id;
  }
  set stage_id(id) {
    this._stage_id = id;
  }

  get integration_id() {
    return super.integration_id;
  }
  set integration_id(id) {
    this._integration_id = id;
  }

  get integration_type() {
    return super.integration_type;
  }
  set integration_type(type) {
    this._integration_type = type;
  }

  get mappings() {
    return super.mappings;
  }
  set mappings(map) {
    if (map === undefined) {
      this._mappings = {
        product: { field_name: undefined, field_id: undefined, regex: "" },
        release: { field_name: undefined, field_id: undefined, regex: "" },
        feature: { field_name: undefined, field_id: undefined, regex: "" },
        sprint: { field_name: undefined, field_id: undefined, regex: "" }
      };
    } else {
      this._mappings = map;
    }
  }

  resetDefinitions() {
    console.log("Resetting mappings");
    this._mappings = {
      product: { field_name: undefined, field_id: undefined, regex: "" },
      release: { field_name: undefined, field_id: undefined, regex: "" },
      feature: { field_name: undefined, field_id: undefined, regex: "" },
      sprint: { field_name: undefined, field_id: undefined, regex: "" }
    };
  }

  static validate(data) {
    let valid = RestMapping.validate(data);
    valid = valid && data.hasOwnProperty("product");
    valid = valid && data.hasOwnProperty("release");
    valid = valid && data.hasOwnProperty("feature");
    valid = valid && data.hasOwnProperty("sprint");
    return valid;
  }

  json() {
    let mappingJson = super.json();
    return {
      ...mappingJson,
      integration_type: this._integration_type
    };
  }

  validate() {
    let valid = super.validate();
    valid =
      valid &&
      this._mappings.product !== undefined &&
      this._mappings.product.field_id !== undefined &&
      this._mappings.product.field_name !== undefined;
    valid =
      valid &&
      this._mappings.feature !== undefined &&
      this._mappings.feature.field_id !== undefined &&
      this._mappings.feature.field_name !== undefined;
    valid =
      valid &&
      this._mappings.sprint !== undefined &&
      this._mappings.sprint.field_id !== undefined &&
      this._mappings.sprint.field_name !== undefined;
    valid =
      valid &&
      this._mappings.release !== undefined &&
      this._mappings.release.field_id !== undefined &&
      this._mappings.release.field_name !== undefined;
    return valid;
  }
}

export class RestGithubMapping extends RestMapping {
  constructor(restData = null) {
    super(restData);
    this._integration_type = "github";
    this._mappings.repos = [];
    if (restData) {
      this._mappings = restData?.mappings;
    }
    this.json = this.json.bind(this);
    this.resetDefinitions = this.resetDefinitions.bind(this);
    this.validate = this.validate.bind(this);
  }

  get repos() {
    return this._mappings.repos;
  }
  set repos(repos) {
    this._mappings.repos = repos;
  }

  get id() {
    return super.id;
  }
  set id(id) {
    this._id = id;
  }

  get stage_id() {
    return super.stage_id;
  }
  set stage_id(id) {
    this._stage_id = id;
  }

  get integration_id() {
    return super.integration_id;
  }
  set integration_id(id) {
    this._integration_id = id;
  }

  get integration_type() {
    return super.integration_type;
  }
  set integration_type(type) {
    this._integration_type = type;
  }

  get mappings() {
    return super.mappings;
  }
  set mappings(map) {
    if (map === undefined) {
      this._mappings = { repos: [] };
    } else {
      this._mappings = map;
    }
  }

  static validate(data) {
    let valid = RestMapping.validate();
    valid =
      valid && data.mappings.hasOwnProperty("repos") && Array.isArray(data.mappings.repos) && data.repos.length > 0;
    if (valid) {
      data.mappings.repos.forEach(repo => {
        valid = valid && repo.hasOwnProperty("repo_name");
        valid = valid && repo.hasOwnProperty("repo_id");
        // valid = valid && repo.hasOwnProperty("branches") && Array.isArray(repo.branches)
        //     && repo.branches.length > 0
      });
    }
    return valid;
  }

  resetDefinitions() {
    this._mappings.repos = [];
  }

  json() {
    let restMapping = super.json();
    return {
      ...restMapping,
      mappings: this._mappings,
      integration_type: this._integration_type
    };
  }

  validate() {
    let valid = super.validate();
    console.log(`super ${valid}`);
    valid = valid && this._mappings.repos && this._mappings.repos.length > 0;
    console.log(`mapping repos length ${valid}`);
    if (!valid) {
      return valid;
    }
    this._mappings.repos.forEach(repo => {
      valid = valid && repo.repo_name !== undefined && repo.repo_id !== undefined;
      // repo.branches !== undefined &&
      // repo.branches.length > 0
      console.log(`mapping repos name and id ${valid}`);
    });
    return valid;
  }
}

export class RestStage {
  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._description = undefined;
    this._type = undefined;
    this._product_id = undefined;
    this._order = undefined;
    this._owner = undefined;
    this._integration_types = [];
    if (restData) {
      this._id = restData?.id;
      this._name = restData?.name;
      this._description = restData?.description;
      this._type = restData?.type;
      this._integration_types = restData?.integration_types;
      this._product_id = restData?.product_id;
      this._owner = restData?.owner;
      this._order = restData?.order;
    }
    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
  }

  get id() {
    return this._id;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get product_id() {
    return this._product_id;
  }
  set product_id(product) {
    this._product_id = product;
  }

  get order() {
    return this._order;
  }
  set order(order) {
    this._order = order;
  }

  get integration_types() {
    return this._integration_types;
  }
  set integration_types(integrations) {
    this._integration_types = integrations;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get type() {
    return this._type;
  }
  set type(type) {
    this._type = type;
  }

  get owner() {
    return this._owner;
  }
  set owner(owner) {
    this._owner = owner;
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      integration_types: this._integration_types,
      product_id: this._product_id,
      order: this._order,
      owner: this._owner,
      description: this._description,
      type: this._type
    };
  }

  validate() {
    let valid = true;
    valid = valid && this._name !== undefined && this._name !== "";
    valid = valid && this._description !== undefined && this._description !== "";
    //valid = valid && this._integrations.length > 0;
    //valid = valid && this._integration_types.length > 0;
    return valid;
  }

  static validate(data) {
    return true;
  }
}

export class RestProduct {
  constructor(restData = null) {
    this._id = undefined;
    this._name = "";
    this._owner = "";
    this._owner_id = undefined;
    this._description = "";
    this._integration_stage = undefined;
    this._key = "";

    if (restData) {
      this._id = restData?.id;
      this._name = restData?.name;
      this._description = restData?.description;
      this._owner = restData?.owner;
      this._key = restData?.key;
      this._owner_id = restData?.owner_id;
    }

    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
  }

  get owner_id() {
    return this._owner_id;
  }

  set owner_id(owner) {
    this._owner_id = owner;
  }

  get id() {
    return this._id;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get key() {
    return this._key;
  }
  set key(key) {
    this._key = key;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get owner() {
    return this._owner;
  }
  set owner(owner) {
    this._owner = owner;
  }

  get integration_stage() {
    return this._integration_stage;
  }
  set integration_stage(stage) {
    this._integration_stage = stage;
  }

  get post_data() {
    return {
      id: this._id,
      name: this._name,
      description: this._description,
      owner: this._owner,
      key: this._key,
      owner_id: this._owner_id
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this.name !== undefined && this.name !== "";
    valid = valid && this._description !== undefined && this._description !== "";
    valid = valid && this._owner !== undefined && this._owner !== "" && validateEmail(this._owner);
    return valid;
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      description: this._description,
      owner: this._owner,
      key: this._key
    };
  }

  validate() {
    let valid = true;
    valid = valid && this.name !== undefined && this.name !== "";
    valid = valid && this._description !== undefined && this._description !== "";
    valid = valid && this._owner !== undefined && this._owner !== "" && validateEmail(this._owner);
    return valid;
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("description");
    //valid = valid && data.hasOwnProperty("owner_id");
    return valid;
  }
}
