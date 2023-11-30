import { get } from "lodash";
import { VARIABLE_SELECT_TYPE, TEXT_TYPE, TEXT_AREA_TYPE, CONIFG_TABLE_FILTER_TYPE } from "constants/fieldTypes";
import {
  ASSESSMENT_CHECK_TYPE,
  CONFIG_TABLE_COLUMN_TYPE,
  CUSTOM_FIELDS_TYPE,
  DATE,
  DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
  DYNAMIC_MULTI_SELECT_TYPE,
  DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  KV_TYPE,
  PASSWORD_STRING
} from "../constants/fieldTypes";

export const OUTPUT_PORT = {
  id: "output",
  type: "output",
  properties: {
    action: "output"
  },
  position: {
    x: 131,
    y: 78
  }
};

export const INPUT_PORT = {
  id: "input",
  type: "input",
  properties: {
    action: "input"
  },
  position: {
    x: 131,
    y: 0
  }
};

export const PRIMITIVE_CONTENT_TYPES = ["string", "number", "date", "epoch", "json_array", "none", "boolean"];

export const flattenSchema = (schema, parent = null) => {
  let flatArray = [];
  // console.log(schema);
  if (!schema) {
    return flatArray;
  }
  Object.keys(schema.fields || {}).forEach(field => {
    const fieldObj = schema.fields[field];
    let dotNotation = fieldObj.key;
    if (parent !== null) {
      dotNotation = `${parent}.${fieldObj.key}`;
    }
    flatArray.push({ variable: dotNotation, content_type: fieldObj.content_type });
    if (fieldObj.hasOwnProperty("fields") && fieldObj.value_type === "json_blob") {
      const childArray = flattenSchema(fieldObj, dotNotation);
      flatArray.push(...childArray);
    }
  });
  return flatArray;
};

const getValueType = fieldType => {
  switch (fieldType) {
    case CONIFG_TABLE_FILTER_TYPE:
      return "json_blob";
    case DATE:
      return "date";
    case KV_TYPE:
    case ASSESSMENT_CHECK_TYPE:
      return "json";
    default:
      return "string";
  }
};

export const getContentType = value => {
  let type = value;
  if (value && value.includes(":")) {
    type = value.split(":")[1];
  }
  return type;
};

export class RestPropelField {
  constructor(data = null) {
    this._key = undefined;
    this._type = undefined;
    this._values = [];
    this._required = false;
    this._hidden = false;
    this._validation = "";
    this._options = [];
    this._default_value = undefined;
    this._dynamic_resource_name = undefined;
    this._search_field = undefined;
    this._display_name = undefined;
    this._description = undefined;
    this._content_type = undefined;
    this._content_schema = undefined;
    this._value_type = undefined;
    this._index = 0;
    this._content_type_from_input = undefined;
    this._content_type_from_input_config_table = undefined;
    this._use_input_fields = undefined;
    this._filters = [];
    if (data !== null) {
      this._key = data.key;
      this._type = data.type || "text";
      this._values = data.values || [];
      this._required = data.required ? data.required : false;
      this._hidden = data.hidden ? data.hidden : false;
      this._validation = data.validation;
      this._options = data.options || [];
      this._dynamic_resource_name = data.dynamic_resource_name;
      this._search_field = data.search_field;
      this._default_value = data.default_value;
      this._display_name = data.display_name;
      this._description = data.description;
      this._index = data.index || 0;
      this._content_type = data.content_type;
      this._content_schema = data.content_schema;
      this._content_type_from_input = data.content_type_from_input;
      this._content_type_from_input_config_table = data.content_type_from_input_config_table;
      this._use_input_fields = data.use_input_fields;
      this._value_type = data.value_type;
      this._filters = data.filters || [];
      if (data.default_value && data.default_value !== "" && this._values && this._values.length === 0) {
        this._values = [
          {
            value: data.default_value,
            type: getValueType(data.type)
          }
        ];
      }
    }
  }

  get filters() {
    return this._filters;
  }

  get use_input_fields() {
    return this._use_input_fields;
  }

  set use_input_fields(fields) {
    this._use_input_fields = fields;
  }

  get content_type_from_input() {
    return this._content_type_from_input;
  }
  set content_type_from_input(input) {
    this._content_type_from_input = input;
  }

  get content_type_from_input_config_table() {
    return this._content_type_from_input_config_table;
  }
  set content_type_from_input_config_table(ct) {
    this._content_type_from_input_config_table = ct;
  }

  get value_type() {
    return this._value_type;
  }
  set value_type(value) {
    this._value_type = value;
  }

  get content_type() {
    return this._content_type;
  }
  set content_type(type) {
    this._content_type = type;
  }

  get content_schema() {
    return this._content_schema;
  }
  set content_schema(schema) {
    this._content_schema = schema;
  }

  get index() {
    return this._index;
  }

  set index(index) {
    this._index = index;
  }

  get key() {
    return this._key;
  }
  get type() {
    return this._type;
  }
  get values() {
    return this._values.map(value => value.value);
  }
  set values(values) {
    if (!Array.isArray(values)) {
      this._values = [];
    } else {
      this._values = values.map(value => ({ value: value, type: getValueType(this._type) }));
    }
  }
  get options() {
    return this._options;
  }
  get default_value() {
    return this._default_value;
  }
  get required() {
    return this._required;
  }
  get hidden() {
    return this._hidden;
  }
  get dynamic_resource_name() {
    return this._dynamic_resource_name;
  }

  get search_field() {
    return this._search_field;
  }

  get display_name() {
    return this._display_name;
  }
  set display_name(name) {
    this._display_name = name;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get json() {
    let values = this._values;
    if (this._type.includes("dynamic")) {
      values = values
        .filter(val => val !== undefined && val.value !== undefined)
        .map(val => {
          if (val.hasOwnProperty("value") && val.value.hasOwnProperty("key")) {
            return { value: val.value.key, type: getValueType(this._type) };
          } else {
            return { value: val.value, type: getValueType(this._type) };
          }
        });
    } else if (this._type === "config-table-filter") {
      if (values && values.length > 0 && values[0].value) {
        //delete values[0].value.schema;
      }
    }
    return {
      key: this._key,
      type: this._type,
      values: values,
      required: this._required,
      hidden: this._hidden,
      validation: this._validation,
      options: this._options,
      default_value: this._default_value,
      dynamic_resource_name: this._dynamic_resource_name,
      search_field: this._search_field,
      display_name: this._display_name,
      description: this._description,
      content_type: this._content_type,
      index: this._index,
      use_input_fields: this._use_input_fields
    };
  }
}

export class RestPropelNode {
  constructor(data = null) {
    this._type = undefined;
    this._id = undefined;
    this._name = "";
    this._description = "";
    this._position = { x: 300, y: 50 };
    this._trigger_type = { label: "manual", key: "manual" };
    this._trigger_event = undefined;
    this._options = [];
    this._display_type = undefined;
    this._ports = {
      input: { ...INPUT_PORT },
      output: { ...OUTPUT_PORT }
    };
    this._input = {};
    this._output = {};
    this._properties = {};
    if (data !== null) {
      this._type = data.type;
      this._id = data.id;
      if (data.ports !== undefined) {
        // console.log("setting ports");
        this._ports = data.ports;
      } else {
        // if(data.id === "0") {
        //   delete this._ports.input;
        // }
      }
      if (data.input !== undefined) {
        this._input = Object.keys(data.input).reduce((map, obj) => {
          map[obj] = new RestPropelField(data.input[obj]);
          return map;
        }, {});
      }
      this._output = data.output || {};
      if (data.position !== undefined) {
        this._position = data.position;
      } else {
        // staggered adding of nodes
        let id = parseInt(data.id);
        let x = 0;
        let y = 0;
        if (data.offset) {
          console.log(`adding offset ${data.offset.x} ${data.offset.y}`);
          x = data.offset.x;
          y = data.offset.y;
        }
        this._position = {
          x: this._position.x - x + id * 10,
          y: this._position.y - y + id * 10
        };
      }
      if (data.name !== undefined) {
        this._name = data.name;
      }
      if (data.properties) {
        this._properties = data.properties;
      }
      if (data.trigger_type) {
        this._trigger_type = data.trigger_type;
      }
      if (data.options) {
        this._options = data.options;
      }
      if (data.trigger_event) {
        this._trigger_event = data.trigger_event;
      }
      this._description = data.description;
      this._display_type = data.display_type;
    }
  }

  get type() {
    return this._type;
  }

  set type(type) {
    this._type = type;
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get position() {
    return this._position;
  }

  set position(position) {
    this._position = position;
  }

  get ports() {
    return this._ports;
  }

  set ports(ports) {
    this._ports = ports;
  }

  get input() {
    return this._input;
  }

  set input(input) {
    this._input = input;
  }

  get output() {
    return this._output;
  }

  set output(output) {
    this._output = output;
  }

  get properties() {
    return this._properties;
  }

  set properties(properties) {
    this._properties = properties;
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get trigger_type() {
    return this._trigger_type;
  }
  set trigger_type(type) {
    this._trigger_type = type;
  }

  get trigger_event() {
    return this._trigger_event;
  }
  set trigger_event(type) {
    this._trigger_event = type;
  }

  get options() {
    return this._options;
  }

  set options(options) {
    this._options = options;
  }

  get display_type() {
    return this._display_type;
  }

  get json() {
    return {
      id: this._id,
      type: this._type,
      description: this._description,
      display_type: this._display_type,
      ports: Object.keys(this._ports).reduce((map, obj) => {
        if (obj === "input") {
          map["input"] = {
            id: "input",
            type: "input",
            properties: {
              action: "input"
            },
            position: {
              x: 131,
              y: 0
            }
          };
          return map;
        } else {
          map["output"] = {
            id: "output",
            type: "output",
            properties: {
              action: "output"
            },
            position: {
              x: 131,
              y: 78
            }
          };
          return map;
        }
      }, {}),
      input: Object.keys(this._input).reduce((map, obj) => {
        map[obj] = this._input[obj].json;
        return map;
      }, {}),
      output: this._output,
      options: this._options,
      position: this._position,
      properties: this._properties,
      name: this._name,
      trigger_type: this._id === "0" ? this._trigger_type : undefined,
      size: {
        //width: 290,
        width: 320,
        height: 124
      }
    };
  }

  get valid() {
    console.log(`Calculating validity for node ${this._name}`);
    let valid = true;
    Object.keys(this._input).forEach(field => {
      if (this._input[field].required && !this._input[field].hidden) {
        valid = valid && this._input[field].values !== undefined && this._input[field].values[0] !== undefined;
      }
    });
    return valid;
  }
}

export class RestPropel {
  static TRIGGERS = ["manual", "ticket", "scheduled"];

  constructor(data = null) {
    this._id = undefined;
    this._permanent_id = undefined;
    this._reload = 1;
    this._nodes = {};
    this._nodes = {
      0: new RestPropelNode({
        id: "0",
        type: "trigger",
        name: "trigger",
        trigger_type: "manual",
        trigger_template_type: "manual",
        description: "Start Trigger for Propel",
        input: {},
        output: {},
        properties: {
          icon: "bell"
        },
        ports: {
          output: { ...OUTPUT_PORT }
        }
      })
    };
    this._links = {};
    this._name = "";
    this._description = "";
    this._trigger_type = RestPropel.TRIGGERS[0];
    this._trigger_template_type = undefined;
    this._enabled = false;
    this._propel_running = false;
    this._stop_propel_runs = false;
    this._nodes_dirty = true;
    this._hovered = {};
    this._offset = { x: 0, y: 0 };
    this._scale = 1;
    this._settings = {};

    if (data !== null) {
      if (data.ui_data) {
        this._nodes = Object.keys(get(data, ["ui_data", "nodes"], {})).reduce((map, node) => {
          const newNode = new RestPropelNode(data.ui_data.nodes[node]);
          map[node] = newNode;
          return map;
        }, {});
        this._links = data.ui_data.links || {};
      }
      this._id = data.id || undefined;
      this._permanent_id = data.permanent_id || undefined;
      this._name = data.name || "";
      this._enabled = data.enabled || false;
      this._propel_running = data.propel_running || false;
      this._stop_propel_runs = data.stop_propel_runs || false;
      this._trigger_type = data.trigger_type;
      this._trigger_template_type = data.trigger_template_type;
      this._description = data.description;
      this._settings = data?.settings || {};
    }

    this.getPredecessorOutputs = this.getPredecessorOutputs.bind(this);
    this.sanitizeAllNodes = this.sanitizeAllNodes.bind(this);
    this.addNodeFromTemplate = this.addNodeFromTemplate.bind(this);
    this.replaceTrigger = this.replaceTrigger.bind(this);
    this.deleteNode = this.deleteNode.bind(this);
    this._findtoNodes = this._findtoNodes.bind(this);
    this.rearrangeNodes = this.rearrangeNodes.bind(this);
    this._findParent = this._findParent.bind(this);
    this.updateOutputContentTypes = this.updateOutputContentTypes.bind(this);
    this.updateInputFields = this.updateInputFields.bind(this);
  }

  get propel_running() {
    return this._propel_running;
  }
  set propel_running(propel_running) {
    this._propel_running = propel_running;
  }

  get stop_propel_runs() {
    return this._stop_propel_runs;
  }

  set stop_propel_runs(stop_propel_runs) {
    this._stop_propel_runs = stop_propel_runs;
  }

  get reload() {
    return this._reload;
  }
  set reload(reload) {
    this._reload = reload;
  }

  set scale(scale) {
    this._scale = scale;
  }
  get scale() {
    return this._scale;
  }

  get offset() {
    return this._offset;
  }

  set offset(offset) {
    this._offset = offset;
  }

  get hovered() {
    return this._hovered;
  }

  set hovered(hovered) {
    this._hovered = hovered;
  }

  get nodes() {
    return this._nodes;
  }

  set nodes(nodes) {
    this._nodes = nodes;
  }

  get links() {
    return this._links;
  }

  set links(links) {
    this._links = links;
  }

  get trigger_type() {
    return this._trigger_type;
  }

  set trigger_type(type) {
    this._trigger_type = type;
  }

  get trigger_template_type() {
    return this._trigger_template_type;
  }

  set trigger_template_type(type) {
    this._trigger_template_type = type;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(en) {
    this._enabled = en;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get nodes_dirty() {
    return this._nodes_dirty;
  }
  set nodes_dirty(dirty) {
    this._nodes_dirty = dirty;
  }

  get id() {
    return this._id;
  }

  get permanentId() {
    return this._permanent_id;
  }

  get newId() {
    return Math.max(...Object.keys(this._nodes)) + 1;
  }

  get settings() {
    return this._settings || {};
  }

  set settings(newSettings) {
    this._settings = newSettings;
  }

  get notifications() {
    return this._settings?.notifications || {};
  }

  set notifications(newSettings) {
    this.settings = { ...this.settings, notifications: newSettings };
  }

  updateInputFields() {
    Object.keys(this._nodes).forEach(nodeId => {
      Object.keys(this._nodes[nodeId].input).forEach(field => {
        const fieldObj = this._nodes[nodeId].input[field];
        if (fieldObj.use_input_fields !== undefined) {
          Object.keys(fieldObj.use_input_fields || {}).forEach(inputField => {
            const inputFieldObj = this._nodes[nodeId].input[fieldObj.use_input_fields[inputField]];
            const inputFieldValues = inputFieldObj.values;
            // take the first value from the array and stick it into the values for this field obj
            if (fieldObj.values.length === 0) {
              fieldObj.values = [
                {
                  [inputField]: inputFieldValues[0]
                }
              ];
            } else {
              fieldObj.values = [
                {
                  ...fieldObj.values[0],
                  [inputField]: inputFieldValues[0]
                }
              ];
            }
          });
        }
      });
    });
  }

  updateOutputContentTypes(nodeId) {
    let updateFlag = false;
    Object.keys(this._nodes[nodeId].output).forEach(field => {
      const fieldObj = this._nodes[nodeId].output[field];
      if (fieldObj.content_type_from_input !== undefined) {
        const inputFieldObj = this._nodes[nodeId].input[fieldObj.content_type_from_input];
        if (inputFieldObj) {
          const values = inputFieldObj.values;
          if (values.length > 0) {
            console.log(`value is set to be ${values[0]}`);
            const re = /\$\{(\d+)\.(\w[\w\d_-]*)((?:\.\w[\w\d_-]*)*)\}/g;
            const matches = values[0].matchAll(re);
            for (const match of matches) {
              const nodeId = match[1];
              const outputField = match[2];
              //console.log(`node id ${nodeId} output field ${outputField}`);
              const outputObj = this._nodes[nodeId].output[outputField];
              //console.log(outputObj);
              // if(outputObj.content_type_from_input !== undefined) {
              //   console.log(outputObj.content_type_from_input);
              // }

              if (!outputObj) {
                fieldObj.content_type = "json_blob";
                fieldObj.content_schema = undefined;
              } else {
                fieldObj.content_type = outputObj.content_type;
                fieldObj.content_schema = outputObj.content_schema;
              }

              if (match[3] && !!outputObj) {
                // there is additional schema related to this. parse that out
                const contentSchema = outputObj.content_schema;
                const fieldPath = match[3]
                  .split(".")
                  .filter(p => p !== "" && p !== undefined)
                  .reduce((arr, obj) => {
                    arr.push("fields", obj);
                    return arr;
                  }, []);
                const schemaObj = get(contentSchema, fieldPath, {});
                console.log(schemaObj);
                //fieldObj.content_type = schemaObj.content_type;
                if (schemaObj) {
                  fieldObj.content_schema = schemaObj;
                }
                updateFlag = true;
              } else {
                if (!outputObj) {
                  fieldObj.content_type = "json_blob";
                  fieldObj.content_schema = undefined;
                } else {
                  fieldObj.content_type = outputObj.content_type;
                  fieldObj.content_schema = outputObj.content_schema;
                }
                updateFlag = true;
              }
              break;
            }
          }
        }
      } else if (fieldObj.content_type_from_input_config_table !== undefined) {
        const inputFieldObj = this._nodes[nodeId].input[fieldObj.content_type_from_input_config_table];
        const schema = get(inputFieldObj.values, [0, "schema"], {});
        // console.log(schema);
        fieldObj.content_type = "json_blob";
        fieldObj.content_schema = schema;
        updateFlag = true;
      }
      // console.log(fieldObj);
    });
    if (updateFlag) {
      this.sanitizeAllNodes();
    }
  }

  getPredecessorOutputs(nodeId) {
    // the logic here will give all the predecessor nodes. Wait can only happen on a node
    // that has been executed before it
    let predecessors = [];
    let currentNode = this._nodes[nodeId];
    while (typeof currentNode !== "undefined") {
      let linkFound = false;
      // eslint-disable-next-line no-loop-func
      Object.keys(this._links).every(link => {
        if (
          this._links[link].to.nodeId !== undefined &&
          this._links[link].to.nodeId.toString() === currentNode.id.toString()
        ) {
          predecessors.push(this._links[link].from.nodeId.toString());
          currentNode = this._nodes[this._links[link].from.nodeId];
          linkFound = true;
          return false;
        }
        return true;
      });
      if (!linkFound) {
        break;
      }
    }
    let predicates = [];
    predecessors.forEach(predecessor => {
      Object.keys(this._nodes[predecessor].output).forEach(field => {
        const outputObj = this._nodes[predecessor].output[field];
        //console.log(outputObj.key);
        //console.log(outputObj.content_type);
        predicates.push({
          // eslint-disable-next-line no-useless-escape
          key: `\$\{${predecessor}.${field}\}`,
          value: `${field}`,
          content_type: outputObj.content_type,
          value_type: outputObj.value_type,
          node: `${predecessor} - ${this._nodes[predecessor].name}`
        });
        if (outputObj.content_schema && outputObj.value_type === "json_blob") {
          const outputs = flattenSchema(outputObj.content_schema, field).map(predicate => ({
            // eslint-disable-next-line no-useless-escape
            key: `\$\{${predecessor}.${predicate.variable}\}`,
            value: `${predicate.variable}`,
            content_type: predicate.content_type,
            value_type: predicate.value_type,
            node: `${predecessor} - ${this._nodes[predecessor].name}`
          }));
          predicates.push(...outputs);
        }
      });
    });
    // predecessors.forEach(predecessor => {
    //   const outputs = Object.keys(this._nodes[predecessor].output).map(predicate => ({
    //     key: `\$\{${predecessor}.${predicate}\}`,
    //     value: `${predicate}`,
    //     node: `${predecessor} - ${this._nodes[predecessor].name}`
    //   }));
    //   predicates.push(...outputs);
    // });
    return predicates;
  }

  sanitizeAllNodes(nodeId = null, isDelete = false) {
    const re = /\$\{(\d+)\.(\w[\w\d_-]*)((?:\.\w[\w\d_-]*)*)\}/g;
    Object.keys(this._nodes).forEach(node => {
      const predicates = this.getPredecessorOutputs(node);
      const predicateNodes = predicates
        .map(predicate => predicate.node.split("-")[0].trim())
        .filter(node => node !== nodeId);
      Object.keys(this._nodes[node].input).forEach(fieldId => {
        const field = this._nodes[node].input[fieldId];
        if ([TEXT_TYPE, TEXT_AREA_TYPE, VARIABLE_SELECT_TYPE].includes(field.type)) {
          let values = field.values;
          values.forEach((val, index) => {
            const matches = val.matchAll(re);

            for (const match of matches) {
              // this is when an entire node or link gets invalidated
              if (!predicateNodes.find(v => v.includes(match[1]))) {
                // Only want to do this when we are deleting nodes
                if (isDelete) {
                  values[index] = undefined;
                }
                break;
              }
              // if (!predicateNodes.includes(match[1])) {
              //   values[index] = undefined;
              //   break;
              // }

              // TODO: Sanitize when some output content type has changed and the predicate is no longer available
              if (!predicates.find(pred => pred.key.includes(`${match[1] || ""}.${match[2] || ""}`))) {
                // Only want to do this when we are deleting nodes
                if (isDelete) {
                  values[index] = undefined;
                }
              }
            }
          });
          field.values = values.filter(val => val !== undefined);
        }
      });
    });
  }

  addNodeFromTemplate(template) {
    let newNodeId = "1";
    if (Object.keys(this._nodes).length > 0) {
      newNodeId = this.newId.toString();
    }
    if (template) {
      const newNode = new RestPropelNode({
        offset: this._offset,
        id: newNodeId,
        type: template.type,
        name: template.name,
        description: template.description,
        input: template.input,
        output: template.output,
        options: template.options,
        display_type: template.display_type,
        content_type: template.content_type,
        content_schema: template.content_schema,
        filters: template.filters || {},
        properties: {
          ...template.ui_data
        }
      });
      const nodes = this._nodes;
      nodes[newNodeId] = newNode;
      console.log(newNode.json);
      return newNodeId;
    }
  }

  replaceTrigger(triggerRecord) {
    this.sanitizeAllNodes("0", true);
    this._nodes["0"].name = triggerRecord.display_name;
    this._nodes["0"].description = triggerRecord.description;
    this._nodes["0"].trigger_type = triggerRecord.type || triggerRecord.trigger_type;
    this._nodes["0"].trigger_template_type = triggerRecord.type;
    this._nodes["0"].properties = { ...triggerRecord.ui_data };
    this._nodes["0"].input = Object.keys(triggerRecord.fields).reduce((map, obj) => {
      map[obj] = new RestPropelField(triggerRecord.fields[obj]);
      return map;
    }, {});
    this._nodes["0"].output = Object.keys(triggerRecord.fields).reduce((map, obj) => {
      map[obj] = triggerRecord.fields[obj];
      return map;
    }, {});

    this._trigger_type = triggerRecord.trigger_type;
    this._trigger_template_type = triggerRecord.type;
  }

  deleteNode(nodeId) {
    Object.keys(this._links).forEach(link => {
      if (this._links[link].to.nodeId === nodeId || this._links[link].from.nodeId === nodeId) {
        delete this._links[link];
      }
    });

    delete this._nodes[nodeId];
    console.log(this._nodes);
    this.sanitizeAllNodes(null, true);
  }

  _findtoNodes(fromNode) {
    let toNodes = [];
    Object.keys(this._links).forEach(linkId => {
      if (this._links[linkId].from.nodeId === fromNode) {
        const toNode = this._links[linkId].to.nodeId;
        if (!toNodes.includes(toNode)) {
          toNodes.push(toNode);
        }
      }
    });
    return toNodes;
  }

  _findParent(node) {
    let parent = "0";
    Object.keys(this._links).forEach(linkId => {
      const toNode = this._links[linkId].to.nodeId;
      if (toNode === node) {
        parent = this._links[linkId].from.nodeId;
      }
    });

    return parent;
  }

  get orderedNodes() {
    let evalNodes = ["0"];
    let currentNodes = ["0"];
    while (true) {
      const nextNodes = evalNodes.reduce((acc, obj) => {
        acc.push(...this._findtoNodes(obj).filter(node => !acc.includes(node)));
        return acc;
      }, []);
      currentNodes.push(...nextNodes);
      if (nextNodes.length === 0) {
        break;
      }
      evalNodes = nextNodes;
    }
    return currentNodes;
  }

  rearrangeNodes() {
    // start out by printing node positions, start with node 0 , thats convinient
    this._nodes_dirty = true;
    let evalNodes = ["0"];
    let currentPosition = 1;
    let nodePositions = {
      0: 0
    };

    const yOffset = 150;
    const nodeXSize = 240;
    const xoffSet = 50;
    const startY = 50;
    const nodeYSize = 20;

    this._nodes["0"].position.x = 600;
    this._nodes["0"].position.y = startY;

    let maxLength = 0;

    while (true) {
      const nextNodes = evalNodes.reduce((acc, obj) => {
        acc.push(...this._findtoNodes(obj).filter(node => !acc.includes(node)));
        return acc;
      }, []);
      if (nextNodes.length === 0) {
        // no more next nodes
        break;
      }
      evalNodes = [...nextNodes.filter(node => !Object.keys(nodePositions).includes(node))];
      maxLength = Math.max(maxLength, evalNodes.length);
      // eslint-disable-next-line no-loop-func
      evalNodes.forEach(node => {
        nodePositions[node] = currentPosition;
        this._nodes[node].position.y = startY + yOffset * currentPosition + (nodeYSize / 2) * (currentPosition - 1);
      });
      currentPosition = currentPosition + 1;
    }

    // now place the first trigger node right in the middle

    for (let i = 1; i < currentPosition; i++) {
      const nodesByPosition = Object.keys(nodePositions).filter(node => nodePositions[node] === i);
      // map nodes by Position according to their parents
      let nodesByParent = {};
      nodesByPosition.forEach(node => {
        const parent = this._findParent(node);
        if (!nodesByParent.hasOwnProperty(parent)) {
          nodesByParent[parent] = [];
        }
        nodesByParent[parent].push(node);
      });
      let overallIndex = 0;
      //const rowLength = maxLength * nodeXSize + (maxLength - 1) * xoffSet;
      const rowLength = nodesByPosition.length * nodeXSize + (nodesByPosition.length - 1) * xoffSet;
      const startX = Math.max(this._nodes["0"].position.x + nodeXSize / 2 - Math.round(rowLength / 2), 50);
      console.log(`${i} length ${rowLength} start ${startX}`);
      let prevX = 0;
      Object.keys(nodesByParent)
        .sort((a, b) => this._nodes[a].position.x - this._nodes[b].position.x)
        .forEach((parent, parentIndex) => {
          const parentRowLength = parent.length * nodeXSize + (parent.length - 1) * xoffSet;
          const parentX = Math.max(
            this._nodes[parent].position.x + nodeXSize / 2 - Math.round(parentRowLength / 2),
            50
          );

          nodesByParent[parent].forEach((node, index) => {
            // get index in the whole row instead of just for the parent ?
            // position relative to start node in the center of the workflow
            const position1 = startX + overallIndex * (xoffSet + nodeXSize);
            // position relative to parent node
            const position2 = parentX + index * (xoffSet + nodeXSize);
            // position relative to previous node
            const position3 = prevX + nodeXSize + xoffSet;
            this._nodes[node].position.x = Math.max(Math.min(position1, position2), position3);
            prevX = this._nodes[node].position.x;
            // for every even parent, offset the y position by some value?
            const yOddOffset = parent.length > 1 ? ((parentIndex % 2) * yOffset) / 2 : 0;
            const xOddOffset = yOddOffset > 0 ? Math.round(nodeXSize / 3) : 0;
            this._nodes[node].position.y = this._nodes[node].position.y + yOddOffset;
            this._nodes[node].position.x = this._nodes[node].position.x - xOddOffset;
            overallIndex = overallIndex + 1;
          });
        });
    }
  }

  get flowchartRepr() {
    let nodesDict = {};
    Object.keys(this._nodes).forEach(id => {
      nodesDict[id] = this._nodes[id].json;
    });

    const data = {
      id: this._id,
      permanent_id: this._permanent_id,
      offset: this._offset,
      //width: "10000px",
      //height: "20000px",
      selected: {},
      hovered: this._hovered,
      name: this._name,
      nodes: nodesDict,
      links: this._links,
      scale: this._scale
    };
    return data;
  }

  get post_data() {
    let nodesDict = {};
    Object.keys(this._nodes).forEach(id => {
      let nodeJSON = this._nodes[id].json;
      Object.keys(nodeJSON.input || {}).forEach(field => {
        if (
          nodeJSON.input[field].type === CONIFG_TABLE_FILTER_TYPE ||
          nodeJSON.input[field].type === CONFIG_TABLE_COLUMN_TYPE
        ) {
          if (nodeJSON.input[field].values && nodeJSON.input[field].values.length > 0) {
            console.log(`deleting schema for ${id} ${field}`);
            delete nodeJSON.input[field].values[0].value.schema;
          }
        } else if (nodeJSON.input[field].type === CUSTOM_FIELDS_TYPE) {
          if (nodeJSON.input[field].values && nodeJSON.input[field].values.length > 0) {
            console.log(nodeJSON.input[field].values);
            const customFields = get(nodeJSON, ["input", field, "values", 0, "value", "custom_fields"], []);
            if (customFields.length === 0) {
              nodeJSON.input[field].values = [];
            }
          }
        }
      });
      //nodesDict[id] = this._nodes[id].json;
      nodesDict[id] = nodeJSON;
    });

    const data = {
      id: this._id,
      permanent_id: this._permanent_id,
      name: this._name,
      description: this._description,
      trigger_type: this._trigger_type,
      trigger_template_type: this._trigger_template_type,
      enabled: this._enabled,
      nodes_dirty: this._nodes_dirty,
      settings: this._settings,
      ui_data: {
        nodes: nodesDict,
        links: this._links
      }
    };

    return data;
  }

  // POST DATA TO EXPORT WITH ALL CONFIGURATION
  // BUT WITHOUT PASSWORD TYPE INPUT VALUES
  get post_data_no_passwords() {
    let nodesDict = {};
    Object.keys(this._nodes).forEach(id => {
      let nodeJSON = this._nodes[id].json;
      Object.keys(nodeJSON.input || {}).forEach(field => {
        if (nodeJSON.input[field].type === PASSWORD_STRING) {
          nodeJSON.input[field].values = [];
        }
      });
      nodesDict[id] = nodeJSON;
    });

    const data = {
      ...this.post_data,
      ui_data: {
        nodes: nodesDict,
        links: this._links
      }
    };

    return data;
  }

  get empty_post_data() {
    let nodesDict = {};
    Object.keys(this._nodes).forEach(id => {
      let nodeJSON = this._nodes[id].json;
      Object.keys(nodeJSON.input || {}).forEach(field => {
        nodeJSON.input[field].values = [];
      });
      nodesDict[id] = nodeJSON;
    });

    const data = {
      id: this._id,
      permanent_id: this._permanent_id,
      name: this._name,
      description: this._description,
      trigger_type: this._trigger_type,
      trigger_template_type: this._trigger_template_type,
      enabled: this._enabled,
      nodes_dirty: this._nodes_dirty,
      ui_data: {
        nodes: nodesDict,
        links: this._links
      }
    };

    return data;
  }

  get static_post_data() {
    let nodesDict = {};
    const dynamicFields = [
      DYNAMIC_SINGLE_SELECT_TYPE,
      DYNAMIC_MULTI_SELECT_TYPE,
      DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE,
      DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
      ASSESSMENT_CHECK_TYPE,
      CONIFG_TABLE_FILTER_TYPE,
      CONFIG_TABLE_COLUMN_TYPE
    ];
    const extraFields = [PASSWORD_STRING];
    const fieldsToExclude = [...dynamicFields, ...extraFields];
    Object.keys(this._nodes).forEach(id => {
      let nodeJSON = this._nodes[id].json;
      Object.keys(nodeJSON.input || {}).forEach(field => {
        if (fieldsToExclude.includes(nodeJSON.input[field].type)) {
          nodeJSON.input[field].values = [];
        }
      });
      nodesDict[id] = nodeJSON;
    });

    const data = {
      id: this._id,
      permanent_id: this._permanent_id,
      name: this._name,
      description: this._description,
      trigger_type: this._trigger_type,
      trigger_template_type: this._trigger_template_type,
      enabled: this._enabled,
      nodes_dirty: this._nodes_dirty,
      ui_data: {
        nodes: nodesDict,
        links: this._links
      }
    };

    return data;
  }

  get valid() {
    let valid = true;
    let errors = [];
    const nodesNum = Object.keys(this._nodes).length;
    const linksNum = Object.keys(this._links).length;
    valid = valid && this._name !== "" && this._name !== undefined;
    if (!valid) {
      errors.push("Name cannot be empty");
    }

    valid = valid && this._name !== "Untitled";
    if (!valid) {
      errors.push("Name cannot be Untitled");
    }
    //valid = valid && this._description !== "" && this._description !== undefined;
    valid = valid && nodesNum > 1;
    if (nodesNum <= 1) {
      errors.push("Need 2 or more nodes to create a propel");
    }
    valid = valid && linksNum > 0;
    if (linksNum <= 0) {
      errors.push("Nodes need to be linked");
    }

    // number of links should be greater than nodes-1. Can be more links with branching
    // but not less
    const nodesConnected = linksNum >= nodesNum - 1;
    valid = valid && nodesConnected;
    if (!nodesConnected) {
      errors.push("Dangling Nodes, all nodes need to be connected");
    }

    // check if configuration of each node is valid
    if (valid) {
      Object.keys(this._nodes).forEach(node => {
        const nodeValid = this._nodes[node].valid;
        valid = valid && nodeValid;
        if (!nodeValid) {
          console.log(JSON.stringify(this._nodes[node].json, null, 4));
          errors.push(`Node ${this._nodes[node].name} is missing configuration`);
        }
      });
    }

    // only one node, the start node does not have a to link. If more than 1 node does not have to link
    // then the node is a dangler

    if (valid) {
      // now make sure there are no dangling nodes
      let fromNodes = [];
      let toNodes = [];
      const allNodes = Object.keys(this._nodes);
      Object.keys(this._links).forEach(link => {
        let fromNodeId = this._links[link].from.nodeId;
        let toNodeId = this._links[link].to.nodeId;
        if (!fromNodes.includes(fromNodeId)) {
          fromNodes.push(fromNodeId);
        }
        if (!toNodes.includes(toNodeId)) {
          toNodes.push(toNodeId);
        }
      });
      const danglers = allNodes.filter(node => !toNodes.includes(node));
      if (danglers.length > 1) {
        valid = false;
        errors.push(`Nodes ${danglers.join(" ")} are not connected to other nodes`);
      }

      // checking for cyclic graphs
    }

    return { result: valid, errors: errors };
  }
}
