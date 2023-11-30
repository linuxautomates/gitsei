import widgetConstants from "dashboard/constants/widgetConstants";

export const multiplexData = (dataHash: any, apiCallArray: Array<any>) => {
  // find all the keys being used in each of the hashes that are not "name"
  let newDataSet = {};
  if (dataHash) {
    // dont transform if there is only 1
    const widgetIds = Object.keys(dataHash);
    if (widgetIds.length === 1 && !apiCallArray[0].composite) {
      return dataHash[widgetIds[0]];
    }
    let currentKeys = {};
    Object.keys(dataHash).forEach((id, index) => {
      const dataSet = dataHash[id];
      const callProps = apiCallArray.find(child => child.id === id);
      // @ts-ignore
      let xUnit = callProps.filters && callProps.filters.across ? callProps.filters.across : undefined;
      const originalXUnit = xUnit;
      if (xUnit && Object.keys(currentKeys).includes(xUnit)) {
        xUnit = `${xUnit}-${index}`;
        // @ts-ignore
        currentKeys[xUnit] = true;
      } else {
        // @ts-ignore
        currentKeys[xUnit] = true;
      }
      // @ts-ignore
      const transformKey = widgetConstants[callProps.reportType]?.transform || undefined;
      // if apart from name, the props are only 1, then use xUnit to replace. Other append it with index
      // @ts-ignore
      const compositeTransform = widgetConstants[callProps.reportType]?.composite_transform || {};
      // console.log(callProps.reportType);
      // console.log(compositeTransform);
      dataSet?.data?.forEach((obj: any, i: number) => {
        let newRow: any = {};
        Object.keys(obj).forEach(key => {
          if (!["min", "max"].includes(key)) {
            if (key === "name") {
              newRow.name = obj.name;
            } else if (key === "key") {
              newRow.key = obj.key;
            } else if (key === transformKey) {
              if (compositeTransform[originalXUnit] !== undefined) {
                let newKey = compositeTransform[originalXUnit];
                //Now dataSet(child data) will be like {data:[...] , widgetName:something}
                //The tooltip key will be now compositeTransformKey_widgetName
                newKey = `${newKey}-${dataSet.widgetName || index}`;

                newRow[newKey] = obj[key];
              } else {
                newRow[xUnit] = obj[key];
              }
            } else if (compositeTransform[key] !== undefined) {
              let compositeTransformKey = `${compositeTransform[key]}-${dataSet.widgetName || index}`;
              if (Object.keys(currentKeys).includes(compositeTransformKey)) {
                compositeTransformKey = `${compositeTransformKey}-${dataSet.widgetName || index}`;
                // @ts-ignore
                currentKeys[compositeTransformKey] = true;
              }
              newRow[compositeTransformKey] = obj[key];
            } else {
              newRow[`${key}-${dataSet.widgetName || index}`] = obj[key];
            }
          }
        });
        // @ts-ignore
        if (newDataSet[obj.name]) {
          // @ts-ignore
          newDataSet[obj.name] = { ...newDataSet[obj.name], ...newRow };
        } else {
          // @ts-ignore
          newDataSet[obj.name] = newRow;
        }
      }, {});
    });
    // @ts-ignore
    return {
      data: Object.keys(newDataSet)
        // @ts-ignore
        .map(key => newDataSet[key])
        .sort((a, b) => a.key - b.key)
    };
  } else {
    return dataHash;
  }
};
