import { Entity } from "model/entities/entity";
import { EntityIdentifier } from "types/entityIdentifier";

export type EntityState<T extends Entity = Entity> = {
  [x in EntityIdentifier]: T;
};
