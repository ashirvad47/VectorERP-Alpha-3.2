package com.example.erpsystem.dto;


import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
@Getter
@Setter
public class UserDTO {
    private String username;
	private String password;
    private String email;
    private String role;
    private MultipartFile image;
    private String imageUrl;
    
    public String getUsername() {
  		return username;
  	}
  	public void setUsername(String username) {
  		this.username = username;
  	}
  	public String getPassword() {
  		return password;
  	}
  	public void setPassword(String password) {
  		this.password = password;
  	}
  	public String getEmail() {
  		return email;
  	}
  	public void setEmail(String email) {
  		this.email = email;
  	}
  	public String getRole() {
  		return role;
  	}
  	public void setRole(String role) {
  		this.role = role;
  	}
	public MultipartFile getImage() {
		return image;
	}
	public void setImage(MultipartFile image) {
		this.image = image;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
