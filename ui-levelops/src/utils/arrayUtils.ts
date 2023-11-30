export const divideArrayIntoChunks = (array: any[], chunkSize: number) => {
  var index = 0;
  var arrayLength = array.length;
  var tempArray = [];

  for (index = 0; index < arrayLength; index += chunkSize) {
    tempArray.push(array.slice(index, index + chunkSize));
  }

  return tempArray;
};

export const convertNumArrayToStringArray = (arr: number[]): string[] => {
  return arr.map(ar => ar.toString());
};

export const moveArrayElement = (arr: any[], from: number, to: number): any[] => {
  if (to < 0 || from < 0) {
    return arr;
  }
  if (to >= arr.length) {
    return arr;
  }
  arr.splice(to, 0, arr.splice(from, 1)[0]);
  return arr;
};

export const appendErrorMessagesHelper = (data: Array<any>) => {
  return Array.isArray(data)
    ? data.reduce((acc, next) => {
        if (!next["success"]) {
          acc = next["error"] + " " + acc;
          return acc;
        }
        return acc;
      }, "")
    : !data["success"]
    ? data["error"]
      ? data["error"]
      : "Error occurred!"
    : "";
};

export const arrayMin = (arr: Array<any>) => {
  return arr?.reduce(function (min: any, cur: any) {
    return cur < min ? cur : min;
  });
};

export const compareValues = (v1: any, v2: any) => {
  if (v1 === v2) {
    return 0;
  }
  return v1 > v2 ? 1 : v1 < v2 ? -1 : 0;
};

export const nestedSort = (data: any = [], key1: any, key2: any) => {
  return data.sort(function (x: any, y: any) {
    let result = compareValues(x?.[key1], y?.[key1]);
    return result === 0 ? compareValues(x?.[key2], y?.[key2]) : result;
  });
};

export const shiftArrayByKey = (data: any = [], key: any, value: any) => {
  return [...data?.filter((elm: any) => elm?.[key] !== value), ...data?.filter((elm: any) => elm?.[key] === value)];
};

export const arrayOfObjFilterAndMap = (arrObj: Array<any>, arr: any) => {
  return arrObj.reduce((acc, value) => {
    if (arr.indexOf(value?.id) !== -1) {
      acc.push(value?.id);
    }
    return acc;
  }, []);
};

export const arrayMove = (arr: any[], fromIndex: number, toIndex: number) => {
  const newArr = [...arr];
  let element = newArr[fromIndex];
  newArr.splice(fromIndex, 1);
  newArr.splice(toIndex, 0, element);
  return newArr;
};
