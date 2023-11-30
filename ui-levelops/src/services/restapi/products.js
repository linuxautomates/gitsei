import BackendService from "./backendService";
import { PRODUCTS } from "constants/restUri";

export class RestProductsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.bulk = this.bulk.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = PRODUCTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  bulk(filter = {}) {
    let url = PRODUCTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = PRODUCTS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PRODUCTS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = PRODUCTS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, product) {
    let url = PRODUCTS.concat("/").concat(id.toString());
    let postData = product.post_data;
    return this.restInstance.put(url, postData, this.options);
  }

  create(product) {
    let postData = product.post_data;
    return this.restInstance.post(PRODUCTS, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(PRODUCTS, { ...this.options, data: ids });
  }
}
