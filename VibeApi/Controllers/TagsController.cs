using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;

namespace VibeApi.Controllers;

[ApiController]
[Route("tags")]
public sealed class TagsController(VibeDbContext dbContext) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<List<TagDto>>> Get()
    {
        return await dbContext.Tags
            .OrderBy(tag => tag.Id)
            .Select(tag => new TagDto(tag.Id, tag.Name))
            .ToListAsync();
    }
}
