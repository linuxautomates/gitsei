export class RestSendComms {

    constructor(restData=null) {
        this._to = undefined;
        this._questionnaire_id = undefined;
        this._kb_id = undefined;
        this._by = undefined;
        this._work_item_id = undefined;

        if(restData !== null) {
            this._to = restData.to;
            this._questionnaire_id = restData.questionnaire_id;
            this._kb_id = restData.kb_id;
            this._work_item_id = restData.work_item_id;
            this._by = restData.by;
        }

        this.json = this.json.bind(this);
    }

    get to() { return this._to}
    set to(to) { this._to = to }

    get questionnaire_id() { return this._questionnaire_id}
    set questionnaire_id(id) { this._questionnaire_id = id}

    get kb_id() { return this._kb_id}
    set kb_id(id) { this._kb_id = id}

    get by() { return this._by}
    set by(by) { this._by = by}

    get work_item_id() { return this._work_item_id}
    set work_item_id(id) { this._work_item_id = id}

    json() {
        return (
            {
                to: this._to,
                questionnaire_id: this._questionnaire_id,
                kb_id: this._kb_id,
                by: this._by,
                work_item_id: this._work_item_id
            }
        );
    }

    static validate(data) {
        let valid = true;
        valid = valid && (data.hasOwnProperty("questionnaire_id") || data.hasOwnProperty("kb_id"));
        valid = valid && data.hasOwnProperty("by");
        valid = valid && data.hasOwnProperty("to");
        valid = valid && data.hasOwnProperty("work_item_id");
        return valid;
    }
}