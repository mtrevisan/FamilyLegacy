package io.github.mtrevisan.familylegacy.v2.ui.components.individual;


/**
 * Immutable DTO containing all displayable information for an individual.
 */
public final class IndividualDisplayInfo{

	private final String id;
	private final String givenName;
	private final String familyName;
	private final String birthDate;      // formatted string
	private final String birthPlace;
	private final String deathDate;      // formatted string
	private final String deathPlace;
	private final Integer age;           // null if unknown
	private final String preferredImageUri;
	private final String crop;           // "x y width height"


	private IndividualDisplayInfo(Builder builder){
		this.id = builder.id;
		this.givenName = builder.givenName;
		this.familyName = builder.familyName;
		this.birthDate = builder.birthDate;
		this.birthPlace = builder.birthPlace;
		this.deathDate = builder.deathDate;
		this.deathPlace = builder.deathPlace;
		this.age = builder.age;
		this.preferredImageUri = builder.preferredImageUri;
		this.crop = builder.crop;
	}

	public boolean isEmpty(){
		return id == null;
	}

	// Getters...
	public String getId(){
		return id;
	}

	public String getGivenName(){
		return givenName;
	}

	public String getFamilyName(){
		return familyName;
	}

	public String getBirthDate(){
		return birthDate;
	}

	public String getBirthPlace(){
		return birthPlace;
	}

	public String getDeathDate(){
		return deathDate;
	}

	public String getDeathPlace(){
		return deathPlace;
	}

	public Integer getAge(){
		return age;
	}

	public String getPreferredImageUri(){
		return preferredImageUri;
	}

	public String getCrop(){
		return crop;
	}

	public static class Builder{
		private String id;
		private String givenName = "";
		private String familyName = "";
		private String birthDate;
		private String birthPlace;
		private String deathDate;
		private String deathPlace;
		private Integer age;
		private String preferredImageUri;
		private String crop;

		public Builder id(String id){
			this.id = id;
			return this;
		}

		public Builder givenName(String givenName){
			this.givenName = givenName != null? givenName: "";
			return this;
		}

		public Builder familyName(String familyName){
			this.familyName = familyName != null? familyName: "";
			return this;
		}

		public Builder birthDate(String birthDate){
			this.birthDate = birthDate;
			return this;
		}

		public Builder birthPlace(String birthPlace){
			this.birthPlace = birthPlace;
			return this;
		}

		public Builder deathDate(String deathDate){
			this.deathDate = deathDate;
			return this;
		}

		public Builder deathPlace(String deathPlace){
			this.deathPlace = deathPlace;
			return this;
		}

		public Builder age(Integer age){
			this.age = age;
			return this;
		}

		public Builder preferredImageUri(String uri){
			this.preferredImageUri = uri;
			return this;
		}

		public Builder crop(String crop){
			this.crop = crop;
			return this;
		}

		public IndividualDisplayInfo build(){
			return new IndividualDisplayInfo(this);
		}
	}

}
