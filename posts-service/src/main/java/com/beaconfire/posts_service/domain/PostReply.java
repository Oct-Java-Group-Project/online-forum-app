package com.beaconfire.posts_service.domain;

import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class PostReply {

    private String replyId;
    private Integer userId;
    @NotBlank(message = "Comment must not be empty")
    private String comment;
    // for deleting the reply
    private boolean isDeleted;
    private Date created_at;
    private Date updated_at;
    private List<SubReply> subReplies;


}

