using Microsoft.EntityFrameworkCore;
using VibeApi.Models;

namespace VibeApi.Data;

public class VibeDbContext(DbContextOptions<VibeDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<Venue> Venues => Set<Venue>();
    public DbSet<Tag> Tags => Set<Tag>();
    public DbSet<VenueTag> VenueTags => Set<VenueTag>();
    public DbSet<Favorite> Favorites => Set<Favorite>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(user => user.Email).IsUnique();
        });

        modelBuilder.Entity<Venue>(entity =>
        {
            entity.HasIndex(venue => venue.ExternalRefId);
            entity.Property(venue => venue.Latitude).HasPrecision(9, 6);
            entity.Property(venue => venue.Longitude).HasPrecision(9, 6);
        });

        modelBuilder.Entity<VenueTag>(entity =>
        {
            entity.HasIndex(venueTag => new { venueTag.VenueId, venueTag.TagId }).IsUnique();

            entity.HasOne(venueTag => venueTag.Venue)
                .WithMany()
                .HasForeignKey(venueTag => venueTag.VenueId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(venueTag => venueTag.Tag)
                .WithMany()
                .HasForeignKey(venueTag => venueTag.TagId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Favorite>(entity =>
        {
            entity.HasIndex(favorite => new { favorite.UserId, favorite.VenueId }).IsUnique();

            entity.HasOne(favorite => favorite.User)
                .WithMany()
                .HasForeignKey(favorite => favorite.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(favorite => favorite.Venue)
                .WithMany()
                .HasForeignKey(favorite => favorite.VenueId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Tag>().HasData(
            new Tag { Id = 1, Name = "Quiet" },
            new Tag { Id = 2, Name = "Social" },
            new Tag { Id = 3, Name = "Focus" },
            new Tag { Id = 4, Name = "Creative" },
            new Tag { Id = 5, Name = "Cheap Wifi" });
    }
}
