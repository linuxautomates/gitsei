export class RestNotes {

    constructor(restData=null) {
        this._id = undefined;
        this._creator = undefined;
        this._body = undefined;
        this._work_item_id = undefined;
        this._created_at = undefined;
        if(restData !== null) {
            this._id = restData.id;
            this._creator = restData.creator;
            this._body = restData.body;
            this._work_item_id = restData.work_item_id;
            this._created_at = restData.created_at;
        }
    }

    get id() { return this._id;}
    set id(id) { this._id = id}

    get creator() { return this._creator}
    set creator(creator) { this._creator = creator}

    get body() { return this._body}
    set body(body) { this._body = body}

    get work_item_id() { return this._work_item_id}
    set work_item_id(id) { this._work_item_id = id}

    get created_at() { return this._created_at}
    set created_at(at) { this._created_at = at}

    json() {
        return (
            {
                id: this._id,
                creator: this._creator,
                created_at: this._created_at,
                body: this._body,
                work_item_id: this._work_item_id
            }
        );
    }

    static validate(data) {
        let valid = true;
        valid = valid && data.hasOwnProperty("id");
        valid = valid && data.hasOwnProperty("creator");
        valid = valid && data.hasOwnProperty("body");
        valid = valid && data.hasOwnProperty("work_item_id");
        valid = valid && data.hasOwnProperty("created_at");

        return valid;
    }
}