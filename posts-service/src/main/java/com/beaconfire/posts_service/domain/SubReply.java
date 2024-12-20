package com.beaconfire.posts_service.domain;

import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubReply {
	
	private String subReplyId;
    private Integer userId;
    @NotBlank(message = "Comment must not be empty")
    private String comment;
    private boolean isDeleted;
    private Date created_at;
    private Date updated_at;

}
