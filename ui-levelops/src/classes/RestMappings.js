import { IntegrationTypes } from "constants/IntegrationTypes";

const SELECT_MAP = {
  github: "repos",
  jira: "projects",
  slack: "channel"
};

export class RestMappings {
  constructor() {
    this._integrationId = undefined;
    this._teamId = undefined;
    this._integrationType = undefined;
    this._selected = {};
    this.addSelect = this.addSelect.bind(this);
    this._makeArray = this._makeArray.bind(this);
    this.json = this.json.bind(this);
  }

  get integrationId() {
    return this._integrationId;
  }

  set integrationId(id) {
    this._integrationId = id;
  }

  get integrationType() {
    return this._integrationType;
  }

  set integrationType(type) {
    this._integrationType = type;
    if (this._selected === {}) {
      switch (type) {
        case IntegrationTypes.GITHUB:
        case IntegrationTypes.JIRA:
          this._selected = { [SELECT_MAP[type]]: {} };
          break;
        case IntegrationTypes.SLACK:
          this._selected = { [SELECT_MAP[type]]: "" };
          break;
      }
    }
  }

  set teamId(id) {
    this._teamId = id;
  }

  get teamId() {
    return this._teamId;
  }

  get selected() {
    return this._selected;
  }

  set selected(select) {
    this._selected = select;
  }

  addSelect(item) {
    if (this._integrationType === undefined) {
      return false;
    }
    switch (this._integrationType) {
      case IntegrationTypes.JIRA:
      case IntegrationTypes.GITHUB:
        let selected = { ...this._selected[[SELECT_MAP[this._integrationType]]], item };
        this._selected[[SELECT_MAP[this._integrationType]]] = selected;
        break;
      case IntegrationTypes.SLACK:
        break;
    }
  }

  _makeArray(selected) {
    let result = {};
    for (var selection in selected) {
      if (selected[selection].constructor === Object) {
        result[selection] = [];
        // this is your repos and projects
        for (var item in selected[selection]) {
          result[selection].push({ name: item, filter: selected[selection][item] });
        }
      } else {
        // this is your slack case for comma separated channels
        result[selection] = selected[selection].split(",");
      }
    }
    // console.log(result);
    return result;
  }

  json() {
    return {
      integration_id: this._integrationId,
      team_id: this._teamId,
      selected: this._makeArray(this._selected)
    };
  }

  static validate(data) {
    // validate is for the list of mappings , we are never mostly going to get a single mapping
    // only update a single mapping
    let result = true;
    result = result && data.hasOwnProperty("records") && data.records.constructor === Array;
    if (!result) {
      return result;
    }
    data.records.forEach(record => {
      result = result && record.hasOwnProperty("integration_name");
      result = result && record.hasOwnProperty("integration_application");
      result = result && record.hasOwnProperty("integration_id");
      result = result && record.hasOwnProperty("selected");
      if (!result) {
        return result;
      }
      // now validate selected
      result = result && record.selected.hasOwnProperty(SELECT_MAP[record.integration_application]);
      if (!result) {
        return result;
      }
      switch (record.integration_application) {
        case IntegrationTypes.GITHUB:
        case IntegrationTypes.JIRA:
          result = result && record.selected[SELECT_MAP[record.integration_application]].constructor === Object;
          break;
        case IntegrationTypes.SLACK:
          result = result && record.selected[SELECT_MAP[record.integration_application]].constructor === String;
          break;
      }
      if (!result) {
        return result;
      }
    });
  }
}
