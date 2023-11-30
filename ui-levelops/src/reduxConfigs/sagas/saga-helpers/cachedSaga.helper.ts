import { forEach } from "lodash";
import { Entity } from "model/entities/entity";
import { EntityState } from "reduxConfigs/reducers/base/EntityState";

/**
 * This function transforms array of records to an object.
 * @example [{id:"2",name:"test"}] => {"2":{id:"2",name:"test"}}
 * @param records
 * @returns A map having id as key and record as value.
 *
 *
 */
export function transformArrayToObjectResponse<T extends Entity>(records: Array<T>): EntityState<T> {
  let transformedResponse: EntityState<T> = {};
  forEach(records, rec => {
    transformedResponse[rec.id] = rec;
  });
  return transformedResponse;
}
