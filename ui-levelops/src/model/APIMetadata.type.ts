import { get } from "lodash";

export class RestAPIMetadata {
  _has_next: boolean;
  _next_page: number;
  _prev_page: number;
  _page: number;
  _page_size: number;
  constructor(restData = null) {
    if (restData) {
      this._has_next = get(restData, "has_next", false);
      this._next_page = get(restData, "next_page", 0);
      this._prev_page = get(restData, "prev_page", 0);
      this._page = get(restData, "page", 0);
      this._page_size = get(restData, "page_size", 10);
    }
  }

  get has_next() {
    return this._has_next;
  }

  get next_page() {
    return this._next_page;
  }

  get prev_page() {
    return this._prev_page;
  }

  get page() {
    return this._page;
  }

  get page_size() {
    return this._page_size;
  }
}
