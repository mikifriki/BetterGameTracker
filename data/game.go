package data

type GameEntry struct {
	HrefTitle string
	GameTitle string `binding:"required"`
	Details   VideoGameDetails
}

type VideoGameDetails struct {
	Description     string `binding:"required"`
	ReleasePlatform string `binding:"required"`
	ReleaseDate     string `binding:"required"`
	Developer       string `binding:"required"`
	MetaRating      string `binding:"required"`
	UserRating      string `binding:"required"`
	PhysicalCopy    string `binding:"required"`
	CoverImage      string `binding:"required"`
}

type PlayEntry struct {
	GameTitle string           `binding:"required"`
	Details   GameEntryDetails `binding:"required"`
}

type PlayEntries struct {
	GameTitle string
	Details   []GameEntryDetails
}

type GameEntryDetails struct {
	Id                int32  `binding:"required"`
	PlaythroughRating string `binding:"required"`
	CompletionDate    string `binding:"required"`
	PlatformPlayedOn  string `binding:"required"`
	TimeToBeat        string `binding:"required"`
	CompletionRate    string `binding:"required"`
	Coop              string
	Location          string `binding:"required"`
	PlayReview        Review
}

type Review struct {
	ReviewDate  string
	ReviewTitle string
	Review      string
	Rating      string
}

type Image struct {
	Location string
	Title    string
}
