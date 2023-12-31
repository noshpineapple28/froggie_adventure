package com.example.restservice.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.restservice.model.Post;
import com.example.restservice.storage.FileSystemStorageService;
import com.example.restservice.model.Comment; 

@Component
public class PostFileDAO implements PostDAO {
    Map<Integer, Post> posts; // mapping for all labs after loaded from JSON
    
    private ObjectMapper objectMapper; 

    private int post_id_count = 0;
    
    private Post latestPost;

    private String filename;

    public PostFileDAO(@Value("${posts.file}") String filename, ObjectMapper objectMapper) throws IOException {
        this.filename = filename;
        
        // sets up the mapper for post persistence
        this.objectMapper = objectMapper;
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Post.class, new PostDeserializer());
        module.addSerializer(Post.class, new PostSerializer());
        this.objectMapper.registerModule(module);

        load();
    }

    private Post[] getPostArray(Integer comparisonTerm) {
        ArrayList<Post> postArrayList = new ArrayList<Post>();

        // iterate through all labs loaded in memory and check if
        // any have the same name as the searched term
        // if the comparisonTerm is null, the program will return all Labs
        for (Post post : posts.values()) {
            // if the sent in comparison term is null
            // return every lab in the labs Map
            // if the current lab itterated onto has the same name as the comparison term
            // add it to the array list of labs
            // else pass over it
            if (comparisonTerm == null || comparisonTerm.equals(post.getId())) {
                postArrayList.add(post);
            }
        }

        // finally transform the compiled list of labs into a array of Labs
        // then return it back to whatever requested a array of labs
        Post[] labArray = new Post[postArrayList.size()];
        postArrayList.toArray(labArray);
        return labArray;
    }

    private Post[] getPostArray() {
        return getPostArray(null);
    }

    private boolean load() throws IOException {
        posts = new TreeMap<>();
        
        // loads all labs from JSON and maps into an array of Labs
        Post[] postArray = objectMapper.readValue(new URL("https://people.rit.edu/nam6711/froggie-adventures/site/media/posts.json"), Post[].class);

        // iterate through the array, placing the current lab into the labs Map
        Post previous = null;
        for (Post post : postArray) { 
            posts.put(post.getId(), post);
            // use to set the post id count where we left off
            if (post.getId() >= this.post_id_count) {
                post_id_count = post.getId() + 1;
                this.latestPost = post;
            } 

            if (previous != null) {
                previous.nextPost = post;
                post.prevPost = previous;
                post.nextPost = post;

                previous = post;
            } else {
                post.prevPost = post;
                post.nextPost = post;

                previous = post;
            }
        }

        // finish
        return true;
    }

    public boolean savePosts() throws IOException {
        // loads all labs into an array for saving to JSON
        Post[] postArray = getPostArray();

        objectMapper.writeValue(new File(filename), postArray);

        // grab file and save to the sftp client
        try {
            FileSystemStorageService fsss = new FileSystemStorageService();
            fsss.store(new File(filename), "");
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (JSchException jSchException) {
            jSchException.printStackTrace();
        } catch (SftpException sftpException) {
            sftpException.printStackTrace();
        }
        return true;
    }

    @Override
    public Integer getPrevious(int post_id) throws IOException {
        synchronized (posts) {
            Post prevPost = posts.get(post_id).prevPost;

            // return prevpost if it exists, otherwise return this post
            return (prevPost != null) ? prevPost.getId() : post_id;
        }
    }

    @Override
    public Integer getNext(int post_id) throws IOException {
        synchronized (posts) {
            Post nextPost = posts.get(post_id).nextPost;

            // return prevpost if it exists, otherwise return this post
            return (nextPost != null) ? nextPost.getId() : post_id;
        }
    }

    @Override
    public int getLatestPostNum() throws IOException {
        synchronized (posts) {
            return this.latestPost.getId();
        }
    }

    @Override
    public Post getLatest() throws IOException {
        synchronized (posts) {
            return this.latestPost;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Post getPost(int post_id) throws IOException {
        synchronized (posts) {
            if (posts.containsKey(post_id)) {
                return posts.get(post_id);
            }
            else {
                return null;
            }
        }
    }

    @Override
    public Post[] getPosts() throws IOException {
        synchronized (posts) {
            return getPostArray(); 
        }
    }

    @Override
    public boolean createPost(Post post) throws IOException {
        synchronized (posts) { 
            post.setId(post_id_count++);
            posts.put(post.getId(), post);

            this.latestPost.nextPost = post;
            post.prevPost = this.latestPost;
            post.nextPost = null;
            this.latestPost = post;

            return savePosts();
        }
    }

    @Override
    public boolean createComment(int post_id, Comment comment) throws IOException {
        synchronized (posts) {
            if (!posts.containsKey(post_id))
                return false;
            
            Post post = posts.get(post_id);
            post.addComment(comment);
            return savePosts();
        }
    }

    @Override
    public Post updatePost(int post_id, Post post) throws IOException {
        synchronized (posts) {
            Post postToUpdate = this.posts.get(post_id);
 
            // update properties
            postToUpdate.setTitle(post.getTitle());
            postToUpdate.setPictureName(post.getPictureName());

            savePosts(); // may throw an IOException
            return post;
        }
    }

    @Override
    public boolean deletePost(int post_id) throws IOException {
        synchronized(posts) {
            if (posts.containsKey(post_id)) {
                Post post = posts.get(post_id);
                // set latest post if THIS is the latest post
                if (this.latestPost == post) {
                    this.latestPost = post.prevPost;
                }    

                // fix linked list order
                if (post.prevPost != null)
                    post.prevPost.nextPost = post.nextPost;
                if (post.nextPost != null)
                    post.nextPost.prevPost = post.prevPost;
                
                posts.remove(post_id);
                return savePosts();
            }
            else
                return false;
        }
    }

    @Override
    public boolean deleteComment(int post_id, int comment_id) throws IOException {
        synchronized(posts) {
            if (posts.containsKey(post_id)) {
                Post post = posts.get(post_id);

                post.deleteComment(comment_id);
                return savePosts();
            }
            else
                return false;
        }
    }
}
