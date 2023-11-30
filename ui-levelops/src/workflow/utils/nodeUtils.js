export const NODE_TYPES = {
  action_email: {
    type: "action_email",
    properties: {
      title: "email",
      icon: "mail",
      configurations: {}
    }
  },
  action_workitem: {
    type: "action_workitem",
    properties: {
      title: "Work Item",
      icon: "shopping",
      configurations: {}
    }
  },
  action_questionnaire: {
    type: "action_questionnaire",
    properties: {
      title: "Questionnaire",
      icon: "reconciliation",
      configurations: {}
    }
  },
  action_kb: {
    type: "action_kb",
    properties: {
      title: "KB Article",
      icon: "file-text",
      configurations: {}
    }
  },
  condition: {
    type: "condition",
    properties: {
      title: "condition",
      icon: "apartment",
      configurations: {
        rules: {
          type: "condition",
          name: "or",
          id: "1",
          children: []
        }
      }
    }
  },
  action_slack: {
    type: "action_slack",
    properties: {
      title: "slack",
      icon: "slack",
      configurations: {}
    }
  },
  wait: {
    type: "wait",
    properties: {
      title: "Check Status",
      icon: "clock-circle",
      configurations: {
        node_id: undefined,
        frequency: {
          type: undefined,
          day_of_month: undefined,
          day_of_week: undefined,
          hour: undefined
        }
      }
    }
  }
};

export function startNode(id, properties = {}, x = 100, y = 100) {
  return {
    id: id,
    type: "start",
    position: {
      x: x,
      y: y
    },
    ports: {
      output: {
        id: "output",
        type: "output",
        properties: {
          action: "output"
        }
      }
    },
    properties: {
      type: "start",
      icon: "info-circle",
      title: "start",
      name: properties.title || "start",
      ...properties
    }
  };
}

export function conditionNode(id, properties = {}, x = 100, y = 100) {
  return {
    id: id,
    type: "condition",
    position: {
      x: x,
      y: y
    },
    ports: {
      input: {
        id: "input",
        type: "input",
        properties: {
          action: "input"
        }
      },
      success: {
        id: "success",
        type: "right",
        properties: {
          action: "success"
        }
      },
      fail: {
        id: "fail",
        type: "output",
        properties: {
          action: "fail"
        }
      }
    },
    properties: {
      icon: "apartment",
      title: "condition",
      type: "condition",
      name: properties.title || "default",
      ...properties
    },
    ui_properties: []
  };
}

export function actionNode(id, properties = {}, type = "action", x = 100, y = 100) {
  return {
    id: id,
    type: type,
    position: {
      x: x,
      y: y
    },
    ports: {
      input: {
        id: "input",
        type: "left",
        properties: {
          action: "input"
        }
      },
      output: {
        id: "output",
        type: "right",
        properties: {
          action: "output"
        }
      }
    },
    properties: {
      icon: "mail",
      type: type,
      name: properties.title || "default",
      ...properties
    }
  };
}

export function waitNode(id, properties = {}, x = 100, y = 100) {
  return {
    id: id,
    type: "wait",
    position: {
      x: x,
      y: y
    },
    ports: {
      input: {
        id: "input",
        type: "left",
        properties: {
          action: "input"
        }
      },
      success: {
        id: "success",
        type: "right",
        properties: {
          action: "success"
        }
      },
      fail: {
        id: "fail",
        type: "output",
        properties: {
          action: "fail"
        }
      }
    },
    properties: {
      icon: "clock-circle",
      type: "wait",
      name: properties.title || "default",
      ...properties
    }
  };
}

export function newWorkFlow() {
  const conditionProperties = {
    ...JSON.parse(JSON.stringify(NODE_TYPES["condition"].properties))
  };
  return {
    offset: {
      x: 0,
      y: 0
    },
    selected: {},
    hovered: {},
    name: "New Workflow",
    nodes: {
      start: startNode("start", {}, 20, 20),
      condition: conditionNode("condition", conditionProperties, 200, 20)
    },
    links: {
      link1: {
        id: "link1",
        from: {
          nodeId: "start",
          portId: "output"
        },
        to: {
          nodeId: "condition",
          portId: "input"
        },
        properties: {
          label: "example link label"
        }
      }
    }
  };
}
