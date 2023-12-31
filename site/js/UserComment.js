class UserComment {
    constructor() {
        this.author = "Input Name";
        this.profile_pic_name = "default_pfp.jpg";
        this.profile_selected;
        this.pfp_select;
        this.description = "Type Something";

        this.html = document.createElement("div");
        this.html.classList = "comment user_comment";

        this.createHTML();
    }

    async createHTML() {
        let div = document.createElement("div");
        div.id = "user_pfp_select";
        this.html.appendChild(div);
        
        this.profile_selected = document.createElement("img");
        this.profile_selected.id = "pfp_image"
        this.profile_selected.src = urlImage + "pfp/" + this.profile_pic_name;
        this.profile_selected.alt = "Profile Picture";
        this.profile_selected.addEventListener("click", () => {
            if (this.pfp_select.style.display === "flex")
                this.pfp_select.style.display = "none";
            else 
                this.pfp_select.style.display = "flex";
        })
        div.appendChild(this.profile_selected);

        // PROFILE PIC
        this.pfp_select = document.createElement("div");
        this.pfp_select.id = "pfp_select"; 
        div.appendChild(this.pfp_select);
        // adds the pfp's 
        this.addPfp(this.pfp_select, "default_pfp.jpg")
        this.addPfp(this.pfp_select, "1.png")
        this.addPfp(this.pfp_select, "frog.png")

        let content = document.createElement("div");
        this.html.appendChild(content);

        let name = document.createElement("input");
        name.type = "text"
        name.id = "name";
        name.placeholder = this.author;
        content.appendChild(name);
        
        let commentSection = document.createElement("div"); 
        commentSection.id = "description_box"
        content.appendChild(commentSection); 

        let comment = document.createElement("textarea");
        comment.id = "comment"; 
        comment.contentEditable = true;
        comment.placeholder = this.description;
        commentSection.appendChild(comment); 

        let button = document.createElement("button");
        button.innerHTML = "Post";
        button.id = "submit";
        button.addEventListener("click", () => this.submitComment(this));
        commentSection.appendChild(button);
    }

    async addPfp(container, src) {
        let img = document.createElement("img");
        img.src = urlImage + "pfp/" + src;
        img.id = src;
        img.classList = "pfp_item";
        img.addEventListener("click", 
            () => {
                this.selectedPfp(img, this.profile_selected);
                this.pfp_select.style.display = "none";
            });
        container.appendChild(img);
    }

    async selectedPfp(selected, comment_pic) {
        comment_pic.src = urlImage + "pfp/" + selected.id;
        this.profile_pic_name = selected.id;
        
        // FIND OLD SELECTED AND REMOVE IT
        let old_selected = document.querySelector(".selected");
        if (old_selected)
            document.querySelector(".selected").classList.remove("selected");        
        
        selected.classList = "selected";
    }

    async submitComment(comment) {
        let json = {
            "id" : 0, // extraneous, will get reassigned by api 
            "author" : document.querySelector("#name").value, 
            "profile_pic_name" : comment.profile_pic_name, 
            "description" : document.querySelector("#comment").value, 
            "date_posted" : null,
        }
        
        let url = urlPost + "addComment/" + postSelected.id;

        const response = await fetch(url, {
            method: 'POST',
            headers: {
            'Content-type': 'application/json'
            },
            body: JSON.stringify(json)
        });

        // allow comment post 
        if (response.status === 201) {
            location.reload();

        } else {
            alert("failed to submit comment, sowwy :(");
        }
    }
}