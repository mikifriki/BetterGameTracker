package data

type GameEntry struct {
	Id        string           `json:"id"`
	HrefTitle string           `json:"hrefTitle"`
	GameTitle string           `binding:"required" json:"gameTitle"`
	Details   VideoGameDetails `json:"details"`
}

type VideoGameDetails struct {
	Description     string `binding:"required" json:"description"`
	ReleasePlatform string `binding:"required" json:"releasePlatform"`
	ReleaseDate     string `binding:"required" json:"releaseDate"`
	Developer       string `binding:"required" json:"developer"`
	MetaRating      string `binding:"required" json:"metaRating"`
	UserRating      string `binding:"required" json:"userRating"`
	PhysicalCopy    string `binding:"required" json:"physicalCopy"`
	CoverImageURL   string `json:"coverImageURL"`
}

type PlayEntry struct {
	GameTitle string           `binding:"required" json:"gameTitle"`
	Details   GameEntryDetails `binding:"required" json:"details"`
}

type PlayEntries struct {
	GameTitle string             `json:"gameTitle"`
	Details   []GameEntryDetails `json:"details"`
}

type GameEntryDetails struct {
	Id                string `json:"id"`
	PlaythroughRating string `binding:"required" json:"playthroughRating"`
	CompletionDate    string `binding:"required" json:"completionDate"`
	PlatformPlayedOn  string `binding:"required" json:"platformPlayedOn"`
	TimeToBeat        string `binding:"required" json:"timeToBeat"`
	CompletionRate    string `binding:"required" json:"completionRate"`
	Coop              string `json:"coop"`
	Location          string `binding:"required" json:"location"`
	PlayReview        string `json:"playReview"`
}

type Review struct {
	ReviewDate  string `json:"reviewDate"`
	ReviewTitle string `json:"reviewTitle"`
	Review      string `json:"review"`
	Rating      string `json:"rating"`
}

type Image struct {
	Location string
	Title    string
}
