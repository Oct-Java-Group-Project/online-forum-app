package com.beaconfire.posts_service.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.beaconfire.posts_service.domain.Accessibility;
import com.beaconfire.posts_service.domain.Metadata;
import com.beaconfire.posts_service.domain.Post;
import com.beaconfire.posts_service.domain.PostReply;
import com.beaconfire.posts_service.domain.SubReply;
import com.beaconfire.posts_service.dto.DataResponse;
import com.beaconfire.posts_service.dto.PostWithUserDTO;
import com.beaconfire.posts_service.dto.UserDTO;
import com.beaconfire.posts_service.dto.UserPermissionsDTO;
import com.beaconfire.posts_service.dto.UserType;
import com.beaconfire.posts_service.exception.PostNotFoundException;
import com.beaconfire.posts_service.exception.ReplyNotFoundException;
import com.beaconfire.posts_service.feign.UserFeignClient;
import com.beaconfire.posts_service.repo.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

//LOOK into getAllPosts , getPostById for interaction with User service
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;
    
    public PostService(PostRepository postRepository, UserFeignClient userFeignClient, ObjectMapper objectMapper ) {
        this.postRepository = postRepository;
        this.userFeignClient= userFeignClient;
        this.objectMapper = objectMapper;
    }
    private UserDTO fetchUserById(Integer userId) {
        try {
            DataResponse response = userFeignClient.getUserById(userId);
            if (response.getSuccess()) {
                return objectMapper.convertValue(response.getData(), UserDTO.class);
            }
        } catch (Exception e) {
            System.err.println("Error fetching user data for ID: " + userId + " - " + e.getMessage());
        }
        return null; // Return null if user data is not found or an error occurs
    }
    
    @Cacheable(value = "posts", key = "#postId", unless = "#result == null")
    public List<PostWithUserDTO> getPostsByUserId(Integer userId) {
        List<Post> posts = postRepository.findByUserId(userId);

        AtomicReference<UserDTO> userRef = new AtomicReference<>(null);

        try {
            DataResponse response = userFeignClient.getUserById(userId);
            if (response.getSuccess()) {
                // Configure ObjectMapper to handle LocalDateTime
                objectMapper.registerModule(new JavaTimeModule());
                
                // Convert data to UserDTO
                UserDTO user = objectMapper.convertValue(response.getData(), UserDTO.class);
                userRef.set(user);
            }
        } catch (Exception e) {
            System.err.println("Error fetching user data: " + e.getMessage());
        }

        return posts.stream()
                .map(post -> PostWithUserDTO.builder()
                        .post(post)
                        .user(userRef.get())
                        .build())
                .collect(Collectors.toList());
    }
    
    public List<Post> getTop3PostsByUser(Integer userId) {
        // Fetch all posts for the given user
        List<Post> userPosts = postRepository.findByUserId(userId);

        // Sort posts by the number of replies in descending order and limit to 3
        return userPosts.stream()
                .sorted((p1, p2) -> {
                    int repliesCount1 = (p1.getPostReplies() != null) ? p1.getPostReplies().size() : 0;
                    int repliesCount2 = (p2.getPostReplies() != null) ? p2.getPostReplies().size() : 0;
                    return Integer.compare(repliesCount2, repliesCount1); // Descending order
                })
                .limit(3) // Take only top 3
                .collect(Collectors.toList());
    }

    public UserPermissionsDTO fetchUserPermissions(Integer userId) {
        try {
            DataResponse response = userFeignClient.getUserById(userId);
            if (response.getSuccess()) {
                objectMapper.registerModule(new JavaTimeModule());
                return objectMapper.convertValue(response.getData(), UserPermissionsDTO.class);
            }
        } catch (Exception e) {
            System.err.println("Error fetching user permissions: " + e.getMessage());
        }
        return null;
    }


    public Post createPost(Post post) {
    	UserPermissionsDTO permissions = fetchUserPermissions(post.getUserId());

        if (permissions == null || !permissions.getActive()) {
            throw new IllegalStateException("User must verify their email to create a post.");
        }
        
    	post.setPostId(UUID.randomUUID().toString());
        post.setCreated_at(new Date());
        post.setUpdated_at(new Date());

        Metadata defaultMetadata = new Metadata();
        defaultMetadata.setViews(0);
        defaultMetadata.setLikes(0);
        defaultMetadata.setCreatedAt(new Date());
        defaultMetadata.setUpdatedAt(new Date());
        post.setMetadata(defaultMetadata);

        return postRepository.save(post);
    }
    public Post likePost(String postId, Integer userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        boolean isAdded = post.getMetadata().getLikesByUsers().add(userId);
        if (isAdded) {
            post.getMetadata().setLikes(post.getMetadata().getLikes() + 1);
            post.getMetadata().setLastActivityAt(new Date());
        } else {
            throw new RuntimeException("User has already liked this post.");
        }

        return postRepository.save(post);
    }

    public Post unlikePost(String postId, Integer userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        boolean isRemoved = post.getMetadata().getLikesByUsers().remove(userId);
        if (isRemoved) {
            post.getMetadata().setLikes(post.getMetadata().getLikes() - 1);
            post.getMetadata().setLastActivityAt(new Date());
        } else {
            throw new RuntimeException("User has not liked this post.");
        }

        return postRepository.save(post);
    }
    public Post incrementViews(String postId) {
        // Fetch the post by ID
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        // Increment the views count in the metadata
        existingPost.getMetadata().setViews(existingPost.getMetadata().getViews() + 1);
        existingPost.getMetadata().setLastActivityAt(new Date());

        // Save and return the updated post
        return postRepository.save(existingPost);
    }



    @CachePut(value = "posts", key = "#postId")
    @CacheEvict(value = "postsList", allEntries = true)
    public PostWithUserDTO updatePost(String postId, Post updatedPost) {
        System.out.println("Fetching post with ID: " + postId);
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        System.out.println("Checking permissions for user: " + updatedPost.getUserId());
        UserPermissionsDTO permissions = fetchUserPermissions(updatedPost.getUserId());
        System.out.println("User Permissions: " + permissions);

        if (permissions == null || !permissions.getActive()) {
            throw new IllegalStateException("User must verify their email to update a post.");
        }

        System.out.println("Updating fields...");
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());
        existingPost.setArchived(updatedPost.isArchived());
        existingPost.setUpdated_at(new Date());

        System.out.println("Saving updated post...");
        Post savedPost = postRepository.save(existingPost);
        System.out.println("Saved Post: " + savedPost);

        // Fetch the associated user and build the DTO
        UserDTO user = fetchUserById(savedPost.getUserId());
        return PostWithUserDTO.builder()
                .post(savedPost)
                .user(user)
                .build();
    }



    public void deletePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));
        post.setAccessibility(Accessibility.DELETED);
        postRepository.save(post);
    }
    
    public Post updateAccessibility(String postId, Accessibility accessibility, Integer currentUserId) {
        // Fetch the post
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        // Fetch user permissions for the current user
        UserPermissionsDTO permissions = fetchUserPermissions(currentUserId);

        Integer postOwnerId = existingPost.getUserId();

        // Authorization logic: Only Admins or Post Owner can modify accessibility
        boolean isAdmin = permissions.getType() == UserType.SUPERADMIN 
                || permissions.getType() == UserType.ADMIN;

        boolean isOwner = postOwnerId.equals(currentUserId);

        System.out.print(permissions.getType());
        System.out.print(isAdmin);
        System.out.print(isOwner);
        if (!(isAdmin || isOwner)) {
            throw new RuntimeException("Unauthorized: You do not have permission to update accessibility.");
        }

        // Accessibility update logic
        if (isAdmin) {
            // Admin-specific logic: Allow only BANNED or PUBLISHED
            if (accessibility == Accessibility.BANNED || accessibility == Accessibility.PUBLISHED) {
                existingPost.setAccessibility(accessibility);
            } else {
                throw new IllegalArgumentException("Admins can only change accessibility to BANNED or PUBLISHED.");
            }
        } else if (isOwner) {
            // Normal User logic: Allow only HIDDEN
            if (accessibility == Accessibility.HIDDEN) {
                existingPost.setAccessibility(accessibility);
            } else {
                throw new IllegalArgumentException("Normal users can only change accessibility to HIDDEN.");
            }
        }

        // Update the 'Last Edited' time
        existingPost.setUpdated_at(new Date());

        return postRepository.save(existingPost);
    }



    @Cacheable(value = "postsList")
    public List<PostWithUserDTO> getAllPosts() {
        List<Post> posts = postRepository.findAll();

        return posts.stream().map(post -> {
            UserDTO user = fetchUserById(post.getUserId());
            return PostWithUserDTO.builder()
                    .post(post)
                    .user(user)
                    .build();
        }).collect(Collectors.toList());
    }


    @Cacheable(value = "posts", key = "#postId", unless = "#result == null")
    public PostWithUserDTO getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        UserDTO user = fetchUserById(post.getUserId());

        return PostWithUserDTO.builder()
                .post(post)
                .user(user)
                .build();
    }


    public Post addReplyToPost(String postId, PostReply reply) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));
        UserPermissionsDTO permissions = fetchUserPermissions(reply.getUserId());

        if (permissions == null || !permissions.getActive()) {
            throw new IllegalStateException("User must verify their email to reply a post.");
        }
        reply.setReplyId(UUID.randomUUID().toString());
        reply.setDeleted(false);
        reply.setCreated_at(new Date());
        reply.setUpdated_at(new Date());

        if (post.getPostReplies() == null) {
            post.setPostReplies(new ArrayList<>());
        }

        post.getPostReplies().add(reply);
        return postRepository.save(post);
    }

    public Post addSubReplyToReply(String postId, String replyId, SubReply subReply) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        PostReply targetReply = post.getPostReplies().stream()
                .filter(reply -> reply.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new ReplyNotFoundException("Reply not found with ID: " + replyId));

        subReply.setSubReplyId(UUID.randomUUID().toString());
        subReply.setDeleted(false);
        subReply.setCreated_at(new Date());
        subReply.setUpdated_at(new Date());

        if (targetReply.getSubReplies() == null) {
            targetReply.setSubReplies(new ArrayList<>());
        }

        targetReply.getSubReplies().add(subReply);
        return postRepository.save(post);
    }
    
    public Post updateMetadata(String postId, Metadata metadata) {
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        // Preserve existing metadata if new metadata fields are null
        Metadata existingMetadata = existingPost.getMetadata();
        if (metadata.getViews() != 0) existingMetadata.setViews(metadata.getViews());
        if (metadata.getLikes() != 0) existingMetadata.setLikes(metadata.getLikes());
        if (metadata.getLikesByUsers() != null) existingMetadata.setLikesByUsers(metadata.getLikesByUsers());

        // Update metadata timestamps
        existingMetadata.setUpdatedAt(new Date());
        existingMetadata.setLastActivityAt(new Date());

        existingPost.setMetadata(existingMetadata);
        existingPost.setUpdated_at(new Date());

        return postRepository.save(existingPost);
    }


    public List<Post> getPostsByAccessibility(Accessibility accessibility) {
        return postRepository.findByAccessibility(accessibility);
    }


    public Post updateReply(String postId, String replyId, PostReply updatedReply) {
        // Find the post by ID
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        // Find the target reply by ID
        PostReply targetReply = existingPost.getPostReplies().stream()
                .filter(reply -> reply.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new PostNotFoundException("Reply not found with ID: " + replyId));

        // Update reply fields
        targetReply.setComment(updatedReply.getComment());
        targetReply.setUpdated_at(new Date());

        // Save and return the updated post
        return postRepository.save(existingPost);
    }
    
    public Post updateSubReply(String postId, String replyId, String subReplyId, SubReply updatedSubReply) {
        // Find the post by ID
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        // Find the target reply by ID
        PostReply targetReply = existingPost.getPostReplies().stream()
                .filter(reply -> reply.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new PostNotFoundException("Reply not found with ID: " + replyId));

        // Find the target sub-reply by ID
        SubReply targetSubReply = targetReply.getSubReplies().stream()
                .filter(subReply -> subReply.getSubReplyId().equals(subReplyId))
                .findFirst()
                .orElseThrow(() -> new PostNotFoundException("Sub-reply not found with ID: " + subReplyId));

        // Update sub-reply fields
        targetSubReply.setComment(updatedSubReply.getComment());
        targetSubReply.setUpdated_at(new Date());

        // Save and return the updated post
        return postRepository.save(existingPost);
    }
    
    
    public Post softDeleteReply(String postId, String replyId, Integer currentUserId) {
        // Find the post by ID
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        // Find the target reply by ID
        PostReply targetReply = existingPost.getPostReplies().stream()
                .filter(reply -> reply.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new ReplyNotFoundException("Reply not found with ID: " + replyId));

        // Fetch post owner details to check authorization
        Integer postOwnerId = existingPost.getUserId(); // Assuming post has userId as the owner
        UserPermissionsDTO permissions = fetchUserPermissions(postOwnerId);
        // Authorization check: Only admin or the post owner can delete
        if (!(permissions.getType().equals("SUPERADMIN") 
                || permissions.getType().equals("ADMIN") 
                || postOwnerId.equals(currentUserId))) {
            throw new RuntimeException("Unauthorized: You do not have permission to delete this reply.");
        }



        // Perform soft delete
        targetReply.setDeleted(true);
        targetReply.setComment("This message has been deleted."); // Set placeholder text
        targetReply.setUpdated_at(new Date());

        // Save and return the updated post
        return postRepository.save(existingPost);
    }
    
    public Post softDeleteSubReply(String postId, String replyId, String subReplyId, Integer currentUserId) {
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        PostReply targetReply = existingPost.getPostReplies().stream()
                .filter(reply -> reply.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new ReplyNotFoundException("Reply not found with ID: " + replyId));

        SubReply targetSubReply = targetReply.getSubReplies().stream()
                .filter(subReply -> subReply.getSubReplyId().equals(subReplyId))
                .findFirst()
                .orElseThrow(() -> new ReplyNotFoundException("Sub-reply not found with ID: " + subReplyId));

        // Authorization check
        Integer postOwnerId = existingPost.getUserId();
        UserPermissionsDTO permissions = fetchUserPermissions(postOwnerId);
        if (!(permissions.getType().equals("SUPERADMIN") 
                || permissions.getType().equals("ADMIN") 
                || postOwnerId.equals(currentUserId))) {
            throw new RuntimeException("Unauthorized: You do not have permission to delete this sub-reply.");
        }

        targetSubReply.setDeleted(true);
        targetSubReply.setComment("This message has been deleted.");
        targetSubReply.setUpdated_at(new Date());

        return postRepository.save(existingPost);
    }
    
    @CachePut(value = "posts", key = "#postId")
    public PostWithUserDTO refreshPostCache(String postId) {
        return getPostById(postId);
    }



    
    


}
