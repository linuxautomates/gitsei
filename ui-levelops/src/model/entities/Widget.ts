import { Entity } from "./entity";

export interface Widget extends Entity {
  query: any;
  type: string;
  metadata: any;
  draft?: boolean;
}
